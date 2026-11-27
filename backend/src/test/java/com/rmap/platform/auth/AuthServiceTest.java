package com.rmap.platform.auth;

import com.rmap.platform.billing.AccessLevel;
import com.rmap.platform.billing.BillingService;
import com.rmap.platform.billing.EntitlementService;
import com.rmap.platform.billing.EntitlementSummary;
import com.rmap.platform.billing.SubscriptionStatus;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.AuditService;
import com.rmap.platform.common.EmailService;
import com.rmap.platform.config.RmapProperties;
import com.rmap.platform.user.Role;
import com.rmap.platform.user.User;
import com.rmap.platform.user.UserRepository;
import com.rmap.platform.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private OtpStore otpStore;
    @Mock private EmailService emailService;
    @Mock private BillingService billingService;
    @Mock private EntitlementService entitlementService;
    @Mock private AuditService auditService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-27T10:00:00Z"), ZoneOffset.UTC);

    private AuthService authService;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        RmapProperties props = new RmapProperties(
                new RmapProperties.SecurityProperties(
                        "test-secret-key-with-more-than-thirty-two-bytes-length",
                        900,
                        604800,
                        600,
                        List.of("http://localhost:5173")
                ),
                new RmapProperties.MailProperties("no-reply@rmap.local"),
                new RmapProperties.BillingProperties(72, "unconfigured", ""),
                new RmapProperties.BootstrapProperties("", "")
        );
        jwtService = new JwtService(props, fixedClock);
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                otpStore,
                emailService,
                jwtService,
                passwordEncoder,
                billingService,
                entitlementService,
                auditService,
                props,
                fixedClock
        );
    }

    @Test
    void registerHashesPasswordAndSendsOtp() {
        when(userRepository.existsByEmailIgnoreCase("dev@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-1");
            return u;
        });
        when(otpStore.generateAndStore("dev@example.com", "EMAIL_VERIFICATION")).thenReturn("482910");

        AuthDtos.RegisterResponse res = authService.register(
                new AuthDtos.RegisterRequest("Dev@Example.com", "StrongPass123!", "Alex Mercer")
        );

        assertThat(res.email()).isEqualTo("dev@example.com");
        assertThat(res.status()).isEqualTo("PENDING_VERIFICATION");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo("StrongPass123!");
        assertThat(passwordEncoder.matches("StrongPass123!", userCaptor.getValue().getPasswordHash())).isTrue();
        verify(emailService).sendOtpEmail("dev@example.com", "482910", "EMAIL_VERIFICATION");
    }

    @Test
    void verifyEmailActivatesAccountProvisionsTrialAndIssuesJwt() {
        User pending = new User("dev@example.com", passwordEncoder.encode("StrongPass123!"), "Alex", Role.USER, UserStatus.PENDING_VERIFICATION, false);
        pending.setId("user-1");

        when(userRepository.findByEmailIgnoreCase("dev@example.com")).thenReturn(Optional.of(pending));
        when(otpStore.verifyAndConsume("dev@example.com", "EMAIL_VERIFICATION", "482910")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(entitlementService.getEntitlement("user-1")).thenReturn(
                new EntitlementSummary(SubscriptionStatus.TRIALING, AccessLevel.FULL_ACCESS, "TRIAL_72H",
                        fixedClock.instant().plusSeconds(72 * 3600L), null, 72, false)
        );

        AuthDtos.AuthTokensResponse tokens = authService.verifyEmail(
                new AuthDtos.VerifyEmailRequest("dev@example.com", "482910")
        );

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.user().emailVerified()).isTrue();
        assertThat(jwtService.parseAndValidate(tokens.accessToken()).userId()).isEqualTo("user-1");
        verify(billingService).provisionTrialIfMissing("user-1");
    }

    @Test
    void refreshReuseRevokesEntireTokenFamily() {
        RefreshToken alreadyUsed = new RefreshToken(
                "user-1",
                "fam-1",
                "unused-in-mock",
                fixedClock.instant().plusSeconds(3600),
                fixedClock.instant().minusSeconds(60)
        );
        alreadyUsed.setRevoked(true);

        RefreshToken sibling = new RefreshToken(
                "user-1",
                "fam-1",
                "sibling-hash",
                fixedClock.instant().plusSeconds(3600),
                fixedClock.instant()
        );

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(alreadyUsed));
        when(refreshTokenRepository.findByFamilyId("fam-1")).thenReturn(List.of(alreadyUsed, sibling));

        assertThatThrownBy(() -> authService.refresh(new AuthDtos.RefreshRequest("reused-raw-token")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("token reuse");

        assertThat(sibling.isRevoked()).isTrue();
        verify(refreshTokenRepository).saveAll(any());
        verify(auditService).record(eq("user-1"), eq(""), eq("AUTH_REFRESH_REUSE_DETECTED"), eq("REFRESH_FAMILY"), eq("fam-1"), any());
    }
}
