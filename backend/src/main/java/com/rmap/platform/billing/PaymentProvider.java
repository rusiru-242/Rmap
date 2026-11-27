package com.rmap.platform.billing;

public interface PaymentProvider {
    String providerName();
    boolean isConfigured();
    CheckoutSessionResult createCheckoutSession(String userId, String email, SubscriptionPlan plan);
    boolean verifyWebhookSignature(String rawPayload, String signatureHeader);

    record CheckoutSessionResult(
            String provider,
            String sessionId,
            String checkoutUrl
    ) {}
}
