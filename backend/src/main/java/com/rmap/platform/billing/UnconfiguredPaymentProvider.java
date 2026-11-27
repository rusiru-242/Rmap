package com.rmap.platform.billing;

import com.rmap.platform.common.ApiException;
import com.rmap.platform.config.RmapProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class UnconfiguredPaymentProvider implements PaymentProvider {
    private final RmapProperties properties;

    public UnconfiguredPaymentProvider(RmapProperties properties) {
        this.properties = properties;
    }

    @Override
    public String providerName() {
        String configured = properties.billing().provider();
        return (configured == null || configured.isBlank()) ? "unconfigured" : configured;
    }

    @Override
    public boolean isConfigured() {
        String secret = properties.billing().webhookSecret();
        return !"unconfigured".equalsIgnoreCase(providerName()) && secret != null && !secret.isBlank();
    }

    @Override
    public CheckoutSessionResult createCheckoutSession(String userId, String email, SubscriptionPlan plan) {
        throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "PAYMENT_PROVIDER_NOT_CONFIGURED",
                "No live payment provider is configured yet. Complete provider eligibility review and set RMAP_PAYMENT_PROVIDER credentials."
        );
    }

    @Override
    public boolean verifyWebhookSignature(String rawPayload, String signatureHeader) {
        String secret = properties.billing().webhookSecret();
        if (secret == null || secret.isBlank() || signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8)));
            String normalized = signatureHeader.trim().replaceFirst("^sha256=", "");
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    normalized.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }
}
