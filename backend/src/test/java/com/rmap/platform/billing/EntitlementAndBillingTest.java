package com.rmap.platform.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.AuditService;
import com.rmap.platform.config.RmapProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitlementAndBillingTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private AuditService auditService;

    private final Instant baseTime = Instant.parse("2026-11-27T10:00:00Z");

    @Test
    void trialProvidesFullAccessFor72HoursThenTransitionsToReadOnly() {
        Subscription trial = new Subscription();
        trial.setUserId("u-1");
        trial.setPlanCode("TRIAL_72H");
        trial.setStatus(SubscriptionStatus.TRIALING);
        trial.setTrialStartedAt(baseTime);
        trial.setTrialEndsAt(baseTime.plus(72, ChronoUnit.HOURS));

        when(subscriptionRepository.findByUserId("u-1")).thenReturn(Optional.of(trial));

        RmapProperties props = propsWithWebhookSecret("");
        PaymentProvider provider = new UnconfiguredPaymentProvider(props);

        EntitlementService duringTrial = new EntitlementService(
                subscriptionRepository, provider, Clock.fixed(baseTime.plus(12, ChronoUnit.HOURS), ZoneOffset.UTC));
        EntitlementSummary active = duringTrial.getEntitlement("u-1");
        assertThat(active.status()).isEqualTo(SubscriptionStatus.TRIALING);
        assertThat(active.accessLevel()).isEqualTo(AccessLevel.FULL_ACCESS);
        assertThat(active.remainingTrialHours()).isEqualTo(60);

        EntitlementService afterTrial = new EntitlementService(
                subscriptionRepository, provider, Clock.fixed(baseTime.plus(73, ChronoUnit.HOURS), ZoneOffset.UTC));
        EntitlementSummary expired = afterTrial.getEntitlement("u-1");
        assertThat(expired.status()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(expired.accessLevel()).isEqualTo(AccessLevel.READ_ONLY);
    }

    @Test
    void unconfiguredProviderBlocksCheckoutAndRejectsUnsignedWebhook() {
        RmapProperties props = propsWithWebhookSecret("whsec_test_secret_123");
        PaymentProvider provider = new UnconfiguredPaymentProvider(props);
        BillingService service = new BillingService(
                subscriptionRepository,
                paymentTransactionRepository,
                provider,
                auditService,
                props,
                new ObjectMapper(),
                Clock.fixed(baseTime, ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> service.startCheckout("u-1", "dev@example.com", "PRO_MONTHLY"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No live payment provider is configured");

        assertThatThrownBy(() -> service.processVerifiedWebhook("stripe", "{\"eventId\":\"evt_1\"}", "bad-sig"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("signature verification failed");
    }

    @Test
    void verifiedWebhookActivatesPaidSubscriptionIdempotently() throws Exception {
        String secret = "whsec_test_secret_123";
        RmapProperties props = propsWithWebhookSecret(secret);
        PaymentProvider provider = new UnconfiguredPaymentProvider(props);
        BillingService service = new BillingService(
                subscriptionRepository,
                paymentTransactionRepository,
                provider,
                auditService,
                props,
                new ObjectMapper(),
                Clock.fixed(baseTime, ZoneOffset.UTC)
        );

        String payload = """
                {"eventId":"evt_999","eventType":"payment.succeeded","userId":"u-1","planCode":"PRO_MONTHLY","amountMinor":2900,"currency":"USD","durationDays":30}
                """.trim();
        String sig = "sha256=" + hmacSha256(secret, payload);

        when(paymentTransactionRepository.existsByProviderEventId("evt_999")).thenReturn(false).thenReturn(true);
        when(subscriptionRepository.findByUserId("u-1")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> first = service.processVerifiedWebhook("testpay", payload, sig);
        assertThat(first.get("status")).isEqualTo("PROCESSED");
        verify(subscriptionRepository).save(any(Subscription.class));

        Map<String, Object> duplicate = service.processVerifiedWebhook("testpay", payload, sig);
        assertThat(duplicate.get("status")).isEqualTo("DUPLICATE_IGNORED");
    }

    private RmapProperties propsWithWebhookSecret(String secret) {
        return new RmapProperties(
                new RmapProperties.SecurityProperties("test-secret-key-with-more-than-thirty-two-bytes-length", 900, 604800, 600, List.of()),
                new RmapProperties.MailProperties("no-reply@rmap.local"),
                new RmapProperties.BillingProperties(72, "unconfigured", secret),
                new RmapProperties.BootstrapProperties("", ""),
                new RmapProperties.AiProperties("", "gemini-2.5-flash", "https://generativelanguage.googleapis.com/v1beta", 45, 2, 250000),
                new RmapProperties.UploadProperties(15728640, 500, 52428800)
        );
    }

    private String hmacSha256(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
