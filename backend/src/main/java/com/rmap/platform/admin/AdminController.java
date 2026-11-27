package com.rmap.platform.admin;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.billing.BillingService;
import com.rmap.platform.billing.PaymentProvider;
import com.rmap.platform.billing.PaymentTransaction;
import com.rmap.platform.billing.PaymentTransactionRepository;
import com.rmap.platform.billing.Subscription;
import com.rmap.platform.billing.SubscriptionRepository;
import com.rmap.platform.billing.SubscriptionStatus;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.ApiResponse;
import com.rmap.platform.common.AuditLog;
import com.rmap.platform.common.AuditLogRepository;
import com.rmap.platform.common.AuditService;
import com.rmap.platform.common.EmailDeliveryLog;
import com.rmap.platform.common.EmailDeliveryLogRepository;
import com.rmap.platform.user.User;
import com.rmap.platform.user.UserRepository;
import com.rmap.platform.user.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final EmailDeliveryLogRepository emailDeliveryLogRepository;
    private final BillingService billingService;
    private final PaymentProvider paymentProvider;
    private final AuditService auditService;

    public AdminController(
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            AuditLogRepository auditLogRepository,
            EmailDeliveryLogRepository emailDeliveryLogRepository,
            BillingService billingService,
            PaymentProvider paymentProvider,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.emailDeliveryLogRepository = emailDeliveryLogRepository;
        this.billingService = billingService;
        this.paymentProvider = paymentProvider;
        this.auditService = auditService;
    }

    public record UpdateUserStatusRequest(@NotNull UserStatus status) {}

    public record ExtendTrialRequest(@Min(1) @Max(720) int extraHours) {}

    public record UpdateSubscriptionRequest(
            @NotBlank String planCode,
            @NotNull SubscriptionStatus status,
            @Min(1) @Max(3650) int durationDays
    ) {}

    public record AdminUserSummary(
            String id,
            String email,
            String fullName,
            String role,
            String status,
            boolean emailVerified,
            Instant createdAt,
            Instant lastLoginAt
    ) {}

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.ok(Map.of(
                "totalUsers", userRepository.count(),
                "totalSubscriptions", subscriptionRepository.count(),
                "totalPayments", paymentTransactionRepository.count(),
                "totalAuditLogs", auditLogRepository.count(),
                "totalEmailLogs", emailDeliveryLogRepository.count(),
                "paymentProvider", paymentProvider.providerName(),
                "paymentProviderConfigured", paymentProvider.isConfigured(),
                "serviceStatus", "UP",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/users")
    public ApiResponse<Page<AdminUserSummary>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<AdminUserSummary> result = userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 100)))
                .map(u -> new AdminUserSummary(
                        u.getId(),
                        u.getEmail(),
                        u.getFullName(),
                        u.getRole().name(),
                        u.getStatus().name(),
                        u.isEmailVerified(),
                        u.getCreatedAt(),
                        u.getLastLoginAt()
                ));
        return ApiResponse.ok(result);
    }

    @PatchMapping("/users/{userId}/status")
    public ApiResponse<AdminUserSummary> updateUserStatus(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
        user.setStatus(request.status());
        user = userRepository.save(user);
        auditService.record(admin.userId(), admin.email(), "ADMIN_USER_STATUS_CHANGED", "USER", userId, Map.of("status", request.status().name()));
        return ApiResponse.ok(new AdminUserSummary(
                user.getId(), user.getEmail(), user.getFullName(), user.getRole().name(),
                user.getStatus().name(), user.isEmailVerified(), user.getCreatedAt(), user.getLastLoginAt()
        ));
    }

    @GetMapping("/subscriptions")
    public ApiResponse<Page<Subscription>> listSubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(subscriptionRepository.findAllByOrderByUpdatedAtDesc(PageRequest.of(page, Math.min(size, 100))));
    }

    @PostMapping("/users/{userId}/trial/extend")
    public ApiResponse<Subscription> extendTrial(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable String userId,
            @Valid @RequestBody ExtendTrialRequest request
    ) {
        return ApiResponse.ok(billingService.adminExtendTrial(admin.userId(), admin.email(), userId, request.extraHours()));
    }

    @PatchMapping("/users/{userId}/subscription")
    public ApiResponse<Subscription> updateSubscription(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable String userId,
            @Valid @RequestBody UpdateSubscriptionRequest request
    ) {
        return ApiResponse.ok(billingService.adminUpdateSubscription(
                admin.userId(), admin.email(), userId, request.planCode(), request.status(), request.durationDays()
        ));
    }

    @GetMapping("/payments")
    public ApiResponse<Page<PaymentTransaction>> listPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(paymentTransactionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 100))));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<Page<AuditLog>> listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        return ApiResponse.ok(auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 100))));
    }

    @GetMapping("/email-logs")
    public ApiResponse<Page<EmailDeliveryLog>> listEmailLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        return ApiResponse.ok(emailDeliveryLogRepository.findAllByOrderBySentAtDesc(PageRequest.of(page, Math.min(size, 100))));
    }
}
