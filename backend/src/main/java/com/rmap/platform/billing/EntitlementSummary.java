package com.rmap.platform.billing;

import java.time.Instant;

public record EntitlementSummary(
        SubscriptionStatus status,
        AccessLevel accessLevel,
        String planCode,
        Instant trialEndsAt,
        Instant currentPeriodEnd,
        long remainingTrialHours,
        boolean paymentProviderConfigured
) {}
