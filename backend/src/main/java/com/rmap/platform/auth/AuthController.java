package com.rmap.platform.auth;

import com.rmap.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthDtos.RegisterResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/resend-otp")
    public ApiResponse<Map<String, String>> resendOtp(@Valid @RequestBody AuthDtos.ResendOtpRequest request) {
        authService.resendVerificationOtp(request);
        return ApiResponse.ok(Map.of("message", "Verification code re-sent."));
    }

    @PostMapping("/verify-email")
    public ApiResponse<AuthDtos.AuthTokensResponse> verifyEmail(@Valid @RequestBody AuthDtos.VerifyEmailRequest request) {
        return ApiResponse.ok(authService.verifyEmail(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.AuthTokensResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthDtos.AuthTokensResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, String>> logout(@Valid @RequestBody AuthDtos.LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.ok(Map.of("message", "Signed out."));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Map<String, String>> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.ok(Map.of("message", "If an account exists for that email, a reset code has been sent."));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Map<String, String>> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok(Map.of("message", "Password updated successfully."));
    }
}
