package com.rmap.platform.auth;

import com.rmap.platform.billing.EntitlementSummary;
import com.rmap.platform.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotBlank @Size(min = 2, max = 100) String fullName
    ) {}

    public record VerifyEmailRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 6) String otpCode
    ) {}

    public record ResendOtpRequest(
            @NotBlank @Email String email
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {}

    public record LogoutRequest(
            @NotBlank String refreshToken
    ) {}

    public record ForgotPasswordRequest(
            @NotBlank @Email String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 6) String otpCode,
            @NotBlank @Size(min = 8, max = 128) String newPassword
    ) {}

    public record RegisterResponse(
            String email,
            String status,
            String message
    ) {}

    public record AuthUserView(
            String id,
            String email,
            String fullName,
            Role role,
            boolean emailVerified
    ) {}

    public record AuthTokensResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInSeconds,
            AuthUserView user,
            EntitlementSummary entitlement
    ) {}
}
