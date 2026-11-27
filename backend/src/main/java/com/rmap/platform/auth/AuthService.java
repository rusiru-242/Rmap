package com.rmap.platform.auth;

import com.rmap.platform.billing.BillingService;
import com.rmap.platform.billing.EntitlementService;
import com.rmap.platform.billing.EntitlementSummary;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.AuditService;
import com.rmap.platform.common.EmailService;
import com.rmap.platform.config.RmapProperties;
import com.rmap.platform.user.Role;
import com.rmap.platform.user.User;
import com.rmap.platform.user.UserRepository;
import com.rmap.platform.user.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    private static final String PURPOSE_VERIFY = "EMAIL_VERIFICATION";
    private static final String PURPOSE_RESET = "PASSWORD_RESET";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpStore otpStore;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final BillingService billingService;
    private final EntitlementService entitlementService;
    private final AuditService auditService;
    private final RmapProperties properties;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            OtpStore otpStore,
            EmailService emailService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            BillingService billingService,
            EntitlementService entitlementService,
            AuditService auditService,
            RmapProperties properties,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.otpStore = otpStore;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.billingService = billingService;
        this.entitlementService = entitlementService;
        this.auditService = auditService;
        this.properties = properties;
        this.clock = clock;
    }

    public AuthDtos.RegisterResponse register(AuthDtos.RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "An account with this email already exists.");
        }

        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                Role.USER,
                UserStatus.PENDING_VERIFICATION,
                false
        );
        user = userRepository.save(user);

        String otp = otpStore.generateAndStore(normalizedEmail, PURPOSE_VERIFY);
        emailService.sendOtpEmail(normalizedEmail, otp, PURPOSE_VERIFY);
        auditService.record(user.getId(), user.getEmail(), "AUTH_REGISTER", "USER", user.getId(), Map.of());

        return new AuthDtos.RegisterResponse(
                normalizedEmail,
                user.getStatus().name(),
                "Verification code sent to your email."
        );
    }

    public void resendVerificationOtp(AuthDtos.ResendOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Account not found."));
        if (user.isEmailVerified()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ALREADY_VERIFIED", "Email is already verified.");
        }
        String otp = otpStore.generateAndStore(normalizedEmail, PURPOSE_VERIFY);
        emailService.sendOtpEmail(normalizedEmail, otp, PURPOSE_VERIFY);
    }

    public AuthDtos.AuthTokensResponse verifyEmail(AuthDtos.VerifyEmailRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Account not found."));

        boolean valid = otpStore.verifyAndConsume(normalizedEmail, PURPOSE_VERIFY, request.otpCode());
        if (!valid) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "Verification code is invalid or expired.");
        }

        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setLastLoginAt(clock.instant());
        user = userRepository.save(user);

        billingService.provisionTrialIfMissing(user.getId());
        auditService.record(user.getId(), user.getEmail(), "AUTH_EMAIL_VERIFIED", "USER", user.getId(), Map.of("trialProvisioned", true));

        return issueTokens(user, UUID.randomUUID().toString());
    }

    public AuthDtos.AuthTokensResponse login(AuthDtos.LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.");
        }
        if (!user.isEmailVerified() || user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "Please verify your email address before signing in.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "This account has been suspended.");
        }

        user.setLastLoginAt(clock.instant());
        userRepository.save(user);
        auditService.record(user.getId(), user.getEmail(), "AUTH_LOGIN", "USER", user.getId(), Map.of());

        return issueTokens(user, UUID.randomUUID().toString());
    }

    public AuthDtos.AuthTokensResponse refresh(AuthDtos.RefreshRequest request) {
        String hash = sha256(request.refreshToken());
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid."));

        Instant now = clock.instant();
        if (existing.isRevoked()) {
            // Reuse of a rotated/revoked refresh token indicates compromise -> revoke entire token family.
            revokeFamily(existing.getFamilyId());
            auditService.record(existing.getUserId(), "", "AUTH_REFRESH_REUSE_DETECTED", "REFRESH_FAMILY", existing.getFamilyId(), Map.of());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSED", "Session revoked due to token reuse. Please sign in again.");
        }

        if (existing.getExpiresAt().isBefore(now)) {
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "Refresh token has expired.");
        }

        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Account no longer exists."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            revokeFamily(existing.getFamilyId());
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE", "Account is not active.");
        }

        String rawNewRefresh = generateOpaqueToken();
        String newHash = sha256(rawNewRefresh);
        existing.setRevoked(true);
        existing.setReplacedByTokenHash(newHash);
        refreshTokenRepository.save(existing);

        RefreshToken replacement = new RefreshToken(
                user.getId(),
                existing.getFamilyId(),
                newHash,
                now.plusSeconds(properties.security().refreshTokenTtlSeconds()),
                now
        );
        refreshTokenRepository.save(replacement);

        return buildTokenResponse(user, rawNewRefresh);
    }

    public void logout(AuthDtos.LogoutRequest request) {
        String hash = sha256(request.refreshToken());
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            revokeFamily(token.getFamilyId());
            auditService.record(token.getUserId(), "", "AUTH_LOGOUT", "REFRESH_FAMILY", token.getFamilyId(), Map.of());
        });
    }

    public void forgotPassword(AuthDtos.ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
            String otp = otpStore.generateAndStore(normalizedEmail, PURPOSE_RESET);
            emailService.sendOtpEmail(normalizedEmail, otp, PURPOSE_RESET);
            auditService.record(user.getId(), user.getEmail(), "AUTH_PASSWORD_RESET_REQUESTED", "USER", user.getId(), Map.of());
        });
    }

    public void resetPassword(AuthDtos.ResetPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "Invalid email or reset code."));

        boolean valid = otpStore.verifyAndConsume(normalizedEmail, PURPOSE_RESET, request.otpCode());
        if (!valid) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "Reset code is invalid or expired.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        revokeAllUserTokens(user.getId());
        auditService.record(user.getId(), user.getEmail(), "AUTH_PASSWORD_RESET_COMPLETED", "USER", user.getId(), Map.of());
    }

    private AuthDtos.AuthTokensResponse issueTokens(User user, String familyId) {
        Instant now = clock.instant();
        String rawRefresh = generateOpaqueToken();
        RefreshToken refreshToken = new RefreshToken(
                user.getId(),
                familyId,
                sha256(rawRefresh),
                now.plusSeconds(properties.security().refreshTokenTtlSeconds()),
                now
        );
        refreshTokenRepository.save(refreshToken);
        return buildTokenResponse(user, rawRefresh);
    }

    private AuthDtos.AuthTokensResponse buildTokenResponse(User user, String rawRefresh) {
        String accessToken = jwtService.generateAccessToken(user);
        EntitlementSummary entitlement = entitlementService.getEntitlement(user.getId());
        return new AuthDtos.AuthTokensResponse(
                accessToken,
                rawRefresh,
                "Bearer",
                jwtService.getAccessTokenTtlSeconds(),
                new AuthDtos.AuthUserView(
                        user.getId(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getRole(),
                        user.isEmailVerified()
                ),
                entitlement
        );
    }

    private void revokeFamily(String familyId) {
        List<RefreshToken> family = refreshTokenRepository.findByFamilyId(familyId);
        for (RefreshToken token : family) {
            token.setRevoked(true);
        }
        refreshTokenRepository.saveAll(family);
    }

    private void revokeAllUserTokens(String userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        for (RefreshToken token : tokens) {
            token.setRevoked(true);
        }
        refreshTokenRepository.saveAll(tokens);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
