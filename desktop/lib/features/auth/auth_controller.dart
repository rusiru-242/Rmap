import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/network/api_client.dart';

class UserSummary {
  final String id;
  final String email;
  final String fullName;
  final String role;
  final bool emailVerified;

  const UserSummary({
    required this.id,
    required this.email,
    required this.fullName,
    required this.role,
    required this.emailVerified,
  });

  factory UserSummary.fromJson(Map<String, dynamic> json) {
    return UserSummary(
      id: json['id']?.toString() ?? '',
      email: json['email']?.toString() ?? '',
      fullName: json['fullName']?.toString() ?? '',
      role: json['role']?.toString() ?? 'USER',
      emailVerified: json['emailVerified'] == true,
    );
  }
}

class EntitlementInfo {
  final String status;
  final String accessLevel;
  final String planCode;
  final int remainingTrialHours;
  final bool paymentProviderConfigured;

  const EntitlementInfo({
    required this.status,
    required this.accessLevel,
    required this.planCode,
    required this.remainingTrialHours,
    required this.paymentProviderConfigured,
  });

  bool get isReadOnly => accessLevel == 'READ_ONLY';

  factory EntitlementInfo.fromJson(Map<String, dynamic> json) {
    return EntitlementInfo(
      status: json['status']?.toString() ?? 'TRIALING',
      accessLevel: json['accessLevel']?.toString() ?? 'FULL_ACCESS',
      planCode: json['planCode']?.toString() ?? 'TRIAL_72H',
      remainingTrialHours: (json['remainingTrialHours'] as num?)?.toInt() ?? 0,
      paymentProviderConfigured: json['paymentProviderConfigured'] == true,
    );
  }
}

class AuthState {
  final bool isAuthenticated;
  final bool isLoading;
  final UserSummary? user;
  final EntitlementInfo? entitlement;
  final String? accessToken;
  final String? refreshToken;
  final String? pendingVerificationEmail;
  final String? errorMessage;
  final String? infoMessage;

  const AuthState({
    this.isAuthenticated = false,
    this.isLoading = false,
    this.user,
    this.entitlement,
    this.accessToken,
    this.refreshToken,
    this.pendingVerificationEmail,
    this.errorMessage,
    this.infoMessage,
  });

  AuthState copyWith({
    bool? isAuthenticated,
    bool? isLoading,
    UserSummary? user,
    EntitlementInfo? entitlement,
    String? accessToken,
    String? refreshToken,
    String? pendingVerificationEmail,
    String? errorMessage,
    String? infoMessage,
    bool clearMessages = false,
  }) {
    return AuthState(
      isAuthenticated: isAuthenticated ?? this.isAuthenticated,
      isLoading: isLoading ?? this.isLoading,
      user: user ?? this.user,
      entitlement: entitlement ?? this.entitlement,
      accessToken: accessToken ?? this.accessToken,
      refreshToken: refreshToken ?? this.refreshToken,
      pendingVerificationEmail: pendingVerificationEmail ?? this.pendingVerificationEmail,
      errorMessage: clearMessages ? null : (errorMessage ?? this.errorMessage),
      infoMessage: clearMessages ? null : (infoMessage ?? this.infoMessage),
    );
  }
}

class AuthController extends StateNotifier<AuthState> {
  final RmapApiClient _api;

  AuthController(this._api) : super(const AuthState());

  Future<bool> register({
    required String fullName,
    required String email,
    required String password,
  }) async {
    state = state.copyWith(isLoading: true, clearMessages: true);
    try {
      await _api.post('/api/v1/auth/register', {
        'fullName': fullName.trim(),
        'email': email.trim(),
        'password': password,
      });
      state = state.copyWith(
        isLoading: false,
        pendingVerificationEmail: email.trim(),
        infoMessage: 'Verification code sent to ${email.trim()}. Check Mailpit (http://localhost:8025) in local dev.',
      );
      return true;
    } on ApiClientException catch (e) {
      state = state.copyWith(isLoading: false, errorMessage: e.message);
      return false;
    }
  }

  Future<bool> verifyEmail({required String email, required String otpCode}) async {
    state = state.copyWith(isLoading: true, clearMessages: true);
    try {
      final res = await _api.post('/api/v1/auth/verify-email', {
        'email': email.trim(),
        'otpCode': otpCode.trim(),
      });
      _applyAuthSuccess(res['data'] as Map<String, dynamic>);
      return true;
    } on ApiClientException catch (e) {
      state = state.copyWith(isLoading: false, errorMessage: e.message);
      return false;
    }
  }

  Future<bool> login({required String email, required String password}) async {
    state = state.copyWith(isLoading: true, clearMessages: true);
    try {
      final res = await _api.post('/api/v1/auth/login', {
        'email': email.trim(),
        'password': password,
      });
      _applyAuthSuccess(res['data'] as Map<String, dynamic>);
      return true;
    } on ApiClientException catch (e) {
      state = state.copyWith(isLoading: false, errorMessage: e.message);
      return false;
    }
  }

  Future<bool> forgotPassword(String email) async {
    state = state.copyWith(isLoading: true, clearMessages: true);
    try {
      await _api.post('/api/v1/auth/forgot-password', {'email': email.trim()});
      state = state.copyWith(
        isLoading: false,
        infoMessage: 'If an account exists for ${email.trim()}, a 6-digit reset code has been sent.',
      );
      return true;
    } on ApiClientException catch (e) {
      state = state.copyWith(isLoading: false, errorMessage: e.message);
      return false;
    }
  }

  Future<bool> resetPassword({
    required String email,
    required String otpCode,
    required String newPassword,
  }) async {
    state = state.copyWith(isLoading: true, clearMessages: true);
    try {
      await _api.post('/api/v1/auth/reset-password', {
        'email': email.trim(),
        'otpCode': otpCode.trim(),
        'newPassword': newPassword,
      });
      state = state.copyWith(
        isLoading: false,
        infoMessage: 'Password updated. You can now sign in with your new password.',
      );
      return true;
    } on ApiClientException catch (e) {
      state = state.copyWith(isLoading: false, errorMessage: e.message);
      return false;
    }
  }

  Future<void> logout() async {
    final rt = state.refreshToken;
    if (rt != null && rt.isNotEmpty) {
      try {
        await _api.post('/api/v1/auth/logout', {'refreshToken': rt});
      } catch (_) {}
    }
    _api.setAccessToken(null);
    state = const AuthState();
  }

  void _applyAuthSuccess(Map<String, dynamic> data) {
    final accessToken = data['accessToken']?.toString();
    final refreshToken = data['refreshToken']?.toString();
    _api.setAccessToken(accessToken);
    state = AuthState(
      isAuthenticated: true,
      isLoading: false,
      accessToken: accessToken,
      refreshToken: refreshToken,
      user: UserSummary.fromJson(data['user'] as Map<String, dynamic>),
      entitlement: data['entitlement'] != null
          ? EntitlementInfo.fromJson(data['entitlement'] as Map<String, dynamic>)
          : null,
    );
  }
}

final authControllerProvider = StateNotifierProvider<AuthController, AuthState>((ref) {
  return AuthController(ref.watch(apiClientProvider));
});
