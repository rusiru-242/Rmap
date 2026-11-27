import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/rmap_tokens.dart';
import 'auth_controller.dart';

enum _AuthViewMode { signIn, register, verifyOtp, resetPassword }

class AuthScreen extends ConsumerStatefulWidget {
  const AuthScreen({super.key});

  @override
  ConsumerState<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends ConsumerState<AuthScreen> {
  _AuthViewMode _mode = _AuthViewMode.signIn;
  final _fullNameCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  final _otpCtrl = TextEditingController();
  final _apiUrlCtrl = TextEditingController(text: 'http://localhost:8080');
  bool _showApiConfig = false;

  @override
  void dispose() {
    _fullNameCtrl.dispose();
    _emailCtrl.dispose();
    _passwordCtrl.dispose();
    _otpCtrl.dispose();
    _apiUrlCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authControllerProvider);
    final theme = Theme.of(context);

    return Scaffold(
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(RmapTokens.space6),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 400),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(RmapTokens.space6),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 28,
                          height: 28,
                          decoration: BoxDecoration(
                            color: theme.colorScheme.primary,
                            borderRadius: BorderRadius.circular(RmapTokens.radiusMd),
                          ),
                          alignment: Alignment.center,
                          child: const Text(
                            'R',
                            style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 15),
                          ),
                        ),
                        const SizedBox(width: RmapTokens.space3),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('Rmap Workspace', style: theme.textTheme.titleMedium, overflow: TextOverflow.ellipsis),
                              Text('AI Software Engineering Platform', style: theme.textTheme.bodySmall, overflow: TextOverflow.ellipsis),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: RmapTokens.space5),
                    SegmentedButton<_AuthViewMode>(
                      segments: const [
                        ButtonSegment(value: _AuthViewMode.signIn, label: Text('Sign in')),
                        ButtonSegment(value: _AuthViewMode.register, label: Text('Register')),
                        ButtonSegment(value: _AuthViewMode.verifyOtp, label: Text('Verify OTP')),
                      ],
                      selected: {_mode == _AuthViewMode.resetPassword ? _AuthViewMode.signIn : _mode},
                      onSelectionChanged: (sel) => setState(() => _mode = sel.first),
                      showSelectedIcon: false,
                    ),
                    const SizedBox(height: RmapTokens.space4),
                    if (auth.errorMessage != null)
                      _BannerBox(message: auth.errorMessage!, color: RmapTokens.danger),
                    if (auth.infoMessage != null)
                      _BannerBox(message: auth.infoMessage!, color: RmapTokens.success),
                    if (_mode == _AuthViewMode.register) ...[
                      Text('Full name', style: theme.textTheme.labelSmall),
                      const SizedBox(height: 4),
                      TextField(controller: _fullNameCtrl, decoration: const InputDecoration(hintText: 'Alex Mercer')),
                      const SizedBox(height: RmapTokens.space3),
                    ],
                    Text('Work or academic email', style: theme.textTheme.labelSmall),
                    const SizedBox(height: 4),
                    TextField(
                      controller: _emailCtrl,
                      keyboardType: TextInputType.emailAddress,
                      decoration: const InputDecoration(hintText: 'you@company.com'),
                    ),
                    const SizedBox(height: RmapTokens.space3),
                    if (_mode == _AuthViewMode.verifyOtp || _mode == _AuthViewMode.resetPassword) ...[
                      Text('6-digit Email OTP', style: theme.textTheme.labelSmall),
                      const SizedBox(height: 4),
                      TextField(
                        controller: _otpCtrl,
                        maxLength: 6,
                        decoration: const InputDecoration(hintText: '123456', counterText: ''),
                      ),
                      const SizedBox(height: RmapTokens.space3),
                    ],
                    if (_mode != _AuthViewMode.verifyOtp) ...[
                      Text(
                        _mode == _AuthViewMode.resetPassword ? 'New password (min 8 chars)' : 'Password',
                        style: theme.textTheme.labelSmall,
                      ),
                      const SizedBox(height: 4),
                      TextField(
                        controller: _passwordCtrl,
                        obscureText: true,
                        decoration: const InputDecoration(hintText: '••••••••'),
                      ),
                      const SizedBox(height: RmapTokens.space4),
                    ],
                    FilledButton(
                      onPressed: auth.isLoading ? null : _submit,
                      child: auth.isLoading
                          ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                          : Text(_submitLabel),
                    ),
                    const SizedBox(height: RmapTokens.space3),
                    Wrap(
                      alignment: WrapAlignment.spaceBetween,
                      spacing: RmapTokens.space2,
                      children: [
                        TextButton(
                          onPressed: () {
                            if (_mode == _AuthViewMode.resetPassword) {
                              setState(() => _mode = _AuthViewMode.signIn);
                            } else {
                              if (_emailCtrl.text.trim().isNotEmpty) {
                                ref.read(authControllerProvider.notifier).forgotPassword(_emailCtrl.text);
                              }
                              setState(() => _mode = _AuthViewMode.resetPassword);
                            }
                          },
                          child: Text(
                            _mode == _AuthViewMode.resetPassword ? 'Back to sign in' : 'Forgot password?',
                            style: theme.textTheme.bodySmall,
                          ),
                        ),
                        TextButton(
                          onPressed: () => setState(() => _showApiConfig = !_showApiConfig),
                          child: Text('Server URL', style: theme.textTheme.bodySmall),
                        ),
                      ],
                    ),
                    if (_showApiConfig) ...[
                      const Divider(height: RmapTokens.space4),
                      Row(
                        children: [
                          Expanded(
                            child: TextField(
                              controller: _apiUrlCtrl,
                              decoration: const InputDecoration(hintText: 'http://localhost:8080'),
                            ),
                          ),
                          const SizedBox(width: RmapTokens.space2),
                          OutlinedButton(
                            onPressed: () {
                              ref.read(apiBaseUrlProvider.notifier).state = _apiUrlCtrl.text.trim();
                            },
                            child: const Text('Apply'),
                          ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  String get _submitLabel => switch (_mode) {
        _AuthViewMode.signIn => 'Sign in to Rmap',
        _AuthViewMode.register => 'Create account & send OTP',
        _AuthViewMode.verifyOtp => 'Verify email & start 72h trial',
        _AuthViewMode.resetPassword => 'Reset password',
      };

  Future<void> _submit() async {
    final notifier = ref.read(authControllerProvider.notifier);
    switch (_mode) {
      case _AuthViewMode.signIn:
        await notifier.login(email: _emailCtrl.text, password: _passwordCtrl.text);
        break;
      case _AuthViewMode.register:
        final ok = await notifier.register(
          fullName: _fullNameCtrl.text,
          email: _emailCtrl.text,
          password: _passwordCtrl.text,
        );
        if (ok && mounted) setState(() => _mode = _AuthViewMode.verifyOtp);
        break;
      case _AuthViewMode.verifyOtp:
        await notifier.verifyEmail(email: _emailCtrl.text, otpCode: _otpCtrl.text);
        break;
      case _AuthViewMode.resetPassword:
        final ok = await notifier.resetPassword(
          email: _emailCtrl.text,
          otpCode: _otpCtrl.text,
          newPassword: _passwordCtrl.text,
        );
        if (ok && mounted) setState(() => _mode = _AuthViewMode.signIn);
        break;
    }
  }
}

class _BannerBox extends StatelessWidget {
  final String message;
  final Color color;

  const _BannerBox({required this.message, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: RmapTokens.space3),
      padding: const EdgeInsets.all(RmapTokens.space3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(RmapTokens.radiusMd),
        border: Border.all(color: color.withValues(alpha: 0.35)),
      ),
      child: Text(message, style: TextStyle(fontSize: 12, color: color)),
    );
  }
}
