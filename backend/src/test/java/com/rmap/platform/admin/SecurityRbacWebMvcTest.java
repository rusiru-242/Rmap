package com.rmap.platform.admin;

import com.rmap.platform.auth.JwtAuthenticationFilter;
import com.rmap.platform.auth.JwtService;
import com.rmap.platform.auth.SecurityConfig;
import com.rmap.platform.billing.BillingService;
import com.rmap.platform.billing.PaymentProvider;
import com.rmap.platform.billing.PaymentTransactionRepository;
import com.rmap.platform.billing.SubscriptionRepository;
import com.rmap.platform.common.AuditLogRepository;
import com.rmap.platform.common.AuditService;
import com.rmap.platform.common.EmailDeliveryLogRepository;
import com.rmap.platform.common.HealthController;
import com.rmap.platform.config.ClockConfig;
import com.rmap.platform.config.RmapProperties;
import com.rmap.platform.user.Role;
import com.rmap.platform.user.User;
import com.rmap.platform.user.UserRepository;
import com.rmap.platform.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {HealthController.class, AdminController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, ClockConfig.class})
@EnableConfigurationProperties(RmapProperties.class)
class SecurityRbacWebMvcTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @MockitoBean private UserRepository userRepository;
    @MockitoBean private SubscriptionRepository subscriptionRepository;
    @MockitoBean private PaymentTransactionRepository paymentTransactionRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private EmailDeliveryLogRepository emailDeliveryLogRepository;
    @MockitoBean private BillingService billingService;
    @MockitoBean private PaymentProvider paymentProvider;
    @MockitoBean private AuditService auditService;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void adminOverviewRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/overview"))
                .andExpect(status().isUnauthorized());

        User regularUser = new User("user@example.com", "hash", "User", Role.USER, UserStatus.ACTIVE, true);
        regularUser.setId("u-1");
        String userToken = jwtService.generateAccessToken(regularUser);

        mockMvc.perform(get("/api/v1/admin/overview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());

        User adminUser = new User("admin@example.com", "hash", "Admin", Role.ADMIN, UserStatus.ACTIVE, true);
        adminUser.setId("a-1");
        String adminToken = jwtService.generateAccessToken(adminUser);

        when(paymentProvider.providerName()).thenReturn("unconfigured");
        when(paymentProvider.isConfigured()).thenReturn(false);

        mockMvc.perform(get("/api/v1/admin/overview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceStatus").value("UP"));
    }
}
