package com.rmap.platform.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.AuditService;
import com.rmap.platform.config.RmapProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class BillingService {
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentProvider paymentProvider;
    private final AuditService auditService;
    private final RmapProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public BillingService(
            SubscriptionRepository subscriptionRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentProvider paymentProvider,
            AuditService auditService,
            RmapProperties properties,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentProvider = paymentProvider;
        this.auditService = auditService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public Subscription provisionTrialIfMissing(String userId) {
        return subscriptionRepository.findByUserId(userId).orElseGet(() -> {
            Instant now = clock.instant();
            int trialHours = properties.billing().trialHours() > 0 ? properties.billing().trialHours() : 72;
            Subscription sub = new Subscription();
            sub.setUserId(userId);
            sub.setPlanCode("TRIAL_72H");
            sub.setStatus(SubscriptionStatus.TRIALING);
            sub.setTrialStartedAt(now);
            sub.setTrialEndsAt(now.plus(trialHours, ChronoUnit.HOURS));
            sub.setProviderName(paymentProvider.providerName());
            return subscriptionRepository.save(sub);
        });
    }

    public List<SubscriptionPlan> getPlans() {
        return SubscriptionPlan.CATALOG;
    }

    public PaymentProvider.CheckoutSessionResult startCheckout(String userId, String email, String planCode) {
        SubscriptionPlan plan = SubscriptionPlan.CATALOG.stream()
                .filter(p -> p.code().equalsIgnoreCase(planCode))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PLAN", "Unknown subscription plan: " + planCode));
        return paymentProvider.createCheckoutSession(userId, email, plan);
    }

    public Map<String, Object> processVerifiedWebhook(String provider, String rawPayload, String signatureHeader) {
        if (!paymentProvider.verifyWebhookSignature(rawPayload, signatureHeader)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE", "Webhook signature verification failed.");
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventId = root.path("eventId").asText("");
            String eventType = root.path("eventType").asText("");
            String userId = root.path("userId").asText("");
            String planCode = root.path("planCode").asText("PRO_MONTHLY");
            long amountMinor = root.path("amountMinor").asLong(0);
            String currency = root.path("currency").asText("USD");
            int durationDays = root.path("durationDays").asInt(30);

            if (eventId.isBlank() || userId.isBlank() || eventType.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_WEBHOOK_PAYLOAD", "Missing required webhook fields.");
            }

            if (paymentTransactionRepository.existsByProviderEventId(eventId)) {
                return Map.of("status", "DUPLICATE_IGNORED", "eventId", eventId);
            }

            Instant now = clock.instant();
            paymentTransactionRepository.save(new PaymentTransaction(
                    userId,
                    provider,
                    eventId,
                    amountMinor,
                    currency,
                    eventType,
                    planCode,
                    now
            ));

            if ("payment.succeeded".equalsIgnoreCase(eventType)) {
                Subscription sub = subscriptionRepository.findByUserId(userId).orElseGet(() -> {
                    Subscription s = new Subscription();
                    s.setUserId(userId);
                    return s;
                });
                sub.setPlanCode(planCode);
                sub.setStatus(SubscriptionStatus.ACTIVE);
                sub.setProviderName(provider);
                sub.setCurrentPeriodStart(now);
                sub.setCurrentPeriodEnd(now.plus(durationDays, ChronoUnit.DAYS));
                subscriptionRepository.save(sub);

                auditService.record("SYSTEM_WEBHOOK", provider, "BILLING_SUBSCRIPTION_ACTIVATED", "SUBSCRIPTION", sub.getId(), Map.of(
                        "userId", userId,
                        "planCode", planCode,
                        "eventId", eventId
                ));
            }

            return Map.of("status", "PROCESSED", "eventId", eventId);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MALFORMED_WEBHOOK_JSON", "Unable to parse webhook payload.");
        }
    }

    public Subscription adminExtendTrial(String adminId, String adminEmail, String targetUserId, int extraHours) {
        Subscription sub = provisionTrialIfMissing(targetUserId);
        Instant base = (sub.getTrialEndsAt() != null && sub.getTrialEndsAt().isAfter(clock.instant()))
                ? sub.getTrialEndsAt()
                : clock.instant();
        sub.setStatus(SubscriptionStatus.TRIALING);
        sub.setTrialEndsAt(base.plus(extraHours, ChronoUnit.HOURS));
        sub = subscriptionRepository.save(sub);
        auditService.record(adminId, adminEmail, "ADMIN_TRIAL_EXTENDED", "SUBSCRIPTION", sub.getId(), Map.of(
                "targetUserId", targetUserId,
                "extraHours", extraHours
        ));
        return sub;
    }

    public Subscription adminUpdateSubscription(String adminId, String adminEmail, String targetUserId, String planCode, SubscriptionStatus status, int durationDays) {
        Subscription sub = provisionTrialIfMissing(targetUserId);
        Instant now = clock.instant();
        sub.setPlanCode(planCode);
        sub.setStatus(status);
        if (status == SubscriptionStatus.ACTIVE) {
            sub.setCurrentPeriodStart(now);
            sub.setCurrentPeriodEnd(now.plus(Math.max(1, durationDays), ChronoUnit.DAYS));
        }
        sub = subscriptionRepository.save(sub);
        auditService.record(adminId, adminEmail, "ADMIN_SUBSCRIPTION_UPDATED", "SUBSCRIPTION", sub.getId(), Map.of(
                "targetUserId", targetUserId,
                "planCode", planCode,
                "status", status.name()
        ));
        return sub;
    }
}
