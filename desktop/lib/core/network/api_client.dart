import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final apiBaseUrlProvider = StateProvider<String>((ref) => 'http://localhost:8080');

class ApiClientException implements Exception {
  final int? statusCode;
  final String code;
  final String message;

  const ApiClientException({
    this.statusCode,
    required this.code,
    required this.message,
  });

  @override
  String toString() => message;
}

class RmapApiClient {
  final Dio _dio;
  String? _accessToken;

  RmapApiClient({required String baseUrl})
      : _dio = Dio(
          BaseOptions(
            baseUrl: baseUrl,
            connectTimeout: const Duration(seconds: 8),
            receiveTimeout: const Duration(seconds: 15),
            headers: const {'Content-Type': 'application/json'},
          ),
        ) {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          if (_accessToken != null && _accessToken!.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $_accessToken';
          }
          handler.next(options);
        },
      ),
    );
  }

  void setAccessToken(String? token) {
    _accessToken = token;
  }

  Future<Map<String, dynamic>> get(String path) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(path);
      return response.data ?? const {};
    } on DioException catch (e) {
      throw _mapError(e);
    }
  }

  Future<Map<String, dynamic>> post(String path, [Map<String, dynamic>? data]) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(path, data: data);
      return response.data ?? const {};
    } on DioException catch (e) {
      throw _mapError(e);
    }
  }

  Future<Map<String, dynamic>> patch(String path, [Map<String, dynamic>? data]) async {
    try {
      final response = await _dio.patch<Map<String, dynamic>>(path, data: data);
      return response.data ?? const {};
    } on DioException catch (e) {
      throw _mapError(e);
    }
  }

  ApiClientException _mapError(DioException e) {
    final status = e.response?.statusCode;
    final data = e.response?.data;
    if (data is Map<String, dynamic>) {
      final detail = data['detail']?.toString() ?? data['message']?.toString();
      final code = data['code']?.toString() ?? data['title']?.toString() ?? 'API_ERROR';
      return ApiClientException(
        statusCode: status,
        code: code,
        message: detail ?? 'Request failed ($status)',
      );
    }
    return ApiClientException(
      statusCode: status,
      code: 'NETWORK_ERROR',
      message: 'Cannot reach Rmap backend (${e.message ?? 'connection failed'}).',
    );
  }
}

final apiClientProvider = Provider<RmapApiClient>((ref) {
  final baseUrl = ref.watch(apiBaseUrlProvider);
  return RmapApiClient(baseUrl: baseUrl);
});
