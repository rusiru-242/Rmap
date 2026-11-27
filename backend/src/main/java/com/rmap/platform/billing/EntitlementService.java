package com.rmap.platform.billing;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class EntitlementService {
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentProvider paymentProvider;
    private final Clock clock;

    public EntitlementService(
            SubscriptionRepository subscriptionRepository,
            PaymentProvider paymentProvider,
            Clock clock
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentProvider = paymentProvider;
        this.clock = clock;
    }

    public EntitlementSummary getEntitlement(String userId) {
        Optional<Subscription> opt = subscriptionRepository.findByUserId(userId);
        if (opt.isEmpty()) {
            return new EntitlementSummary(
                    SubscriptionStatus.EXPIRED,
                    AccessLevel.READ_ONLY,
                    "NONE",
                    null,
                    null,
                    0,
                    paymentProvider.isConfigured()
            );
        }

        Subscription sub = opt.get();
        Instant now = clock.instant();

        if (sub.getStatus() == SubscriptionStatus.ACTIVE
                && sub.getCurrentPeriodEnd() != null
                && sub.getCurrentPeriodEnd().isAfter(now)) {
            return new EntitlementSummary(
                    SubscriptionStatus.ACTIVE,
                    AccessLevel.FULL_ACCESS,
                    sub.getPlanCode(),
                    sub.getTrialEndsAt(),
                    sub.getCurrentPeriodEnd(),
                    0,
                    paymentProvider.isConfigured()
            );
        }

        if (sub.getStatus() == SubscriptionStatus.TRIALING
                && sub.getTrialEndsAt() != null
                && sub.getTrialEndsAt().isAfter(now)) {
            long remainingHours = Math.max(1, Duration.between(now, sub.getTrialEndsAt()).toHours());
            return new EntitlementSummary(
                    SubscriptionStatus.TRIALING,
                    AccessLevel.FULL_ACCESS,
                    sub.getPlanCode(),
                    sub.getTrialEndsAt(),
                    sub.getCurrentPeriodEnd(),
                    remainingHours,
                    paymentProvider.isConfigured()
            );
        }

        return new EntitlementSummary(
                SubscriptionStatus.EXPIRED,
                AccessLevel.READ_ONLY,
                sub.getPlanCode(),
                sub.getTrialEndsAt(),
                sub.getCurrentPeriodEnd(),
                0,
                paymentProvider.isConfigured()
        );
    }

    public boolean hasFullAccess(String userId) {
        return getEntitlement(userId).accessLevel() == AccessLevel.FULL_ACCESS;
    }
}
