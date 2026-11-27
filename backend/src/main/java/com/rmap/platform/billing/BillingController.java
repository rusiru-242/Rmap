package com.rmap.platform.billing;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {
    private final BillingService billingService;
    private final EntitlementService entitlementService;

    public BillingController(BillingService billingService, EntitlementService entitlementService) {
        this.billingService = billingService;
        this.entitlementService = entitlementService;
    }

    public record CheckoutRequest(@NotBlank String planCode) {}

    @GetMapping("/plans")
    public ApiResponse<List<SubscriptionPlan>> plans() {
        return ApiResponse.ok(billingService.getPlans());
    }

    @GetMapping("/entitlement")
    public ApiResponse<EntitlementSummary> entitlement(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(entitlementService.getEntitlement(user.userId()));
    }

    @PostMapping("/checkout")
    public ApiResponse<PaymentProvider.CheckoutSessionResult> checkout(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CheckoutRequest request
    ) {
        return ApiResponse.ok(billingService.startCheckout(user.userId(), user.email(), request.planCode()));
    }

    @PostMapping("/webhooks/{provider}")
    public ApiResponse<Map<String, Object>> webhook(
            @PathVariable String provider,
            @RequestHeader(value = "X-Rmap-Signature", required = false) String signature,
            @RequestBody String rawPayload
    ) {
        return ApiResponse.ok(billingService.processVerifiedWebhook(provider, rawPayload, signature));
    }
}
