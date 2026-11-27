package com.rmap.platform;

import com.rmap.platform.auth.AuthController;
import com.rmap.platform.auth.AuthDtos;
import com.rmap.platform.auth.AuthService;
import com.rmap.platform.auth.JwtAuthenticationFilter;
import com.rmap.platform.auth.JwtService;
import com.rmap.platform.auth.SecurityConfig;
import com.rmap.platform.billing.BillingController;
import com.rmap.platform.billing.BillingService;
import com.rmap.platform.billing.EntitlementAspect;
import com.rmap.platform.billing.EntitlementService;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.AuditService;
import com.rmap.platform.common.GlobalExceptionHandler;
import com.rmap.platform.config.ClockConfig;
import com.rmap.platform.config.RmapProperties;
import com.rmap.platform.user.Role;
import com.rmap.platform.user.User;
import com.rmap.platform.user.UserStatus;
import com.rmap.platform.workspace.LocalWorkspaceRegistration;
import com.rmap.platform.workspace.LocalWorkspaceRepository;
import com.rmap.platform.workspace.WorkspaceController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, BillingController.class, WorkspaceController.class})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        ClockConfig.class,
        GlobalExceptionHandler.class,
        AopAutoConfiguration.class,
        EntitlementAspect.class
})
@EnableConfigurationProperties(RmapProperties.class)
class Phase1IntegrationWebMvcTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @MockitoBean private AuthService authService;
    @MockitoBean private BillingService billingService;
    @MockitoBean private EntitlementService entitlementService;
    @MockitoBean private LocalWorkspaceRepository localWorkspaceRepository;
    @MockitoBean private AuditService auditService;

    @Test
    void registerRejectsInvalidDtoWithProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"123","fullName":"A"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void registerReturnsCreatedOnValidPayload() throws Exception {
        when(authService.register(any())).thenReturn(
                new AuthDtos.RegisterResponse("dev@example.com", "PENDING_VERIFICATION", "Verification code sent.")
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dev@example.com","password":"StrongPassword1!","fullName":"Alex Mercer"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));
    }

    @Test
    void workspaceRegistrationEnforcesActiveEntitlementAndBlocksExpiredTrial() throws Exception {
        User user = new User("trial@example.com", "hash", "Trial User", Role.USER, UserStatus.ACTIVE, true);
        user.setId("u-trial");
        String token = jwtService.generateAccessToken(user);

        String payload = """
                {
                  "displayName": "sample-repo",
                  "pathHash": "abcdef0123456789abcdef0123456789",
                  "detectedStacks": ["Java / JVM"],
                  "indexedFileCount": 12,
                  "excludedSensitiveFileCount": 1
                }
                """;

        when(entitlementService.hasFullAccess("u-trial")).thenReturn(false);
        mockMvc.perform(post("/api/v1/workspaces")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ENTITLEMENT_READ_ONLY"));

        when(entitlementService.hasFullAccess("u-trial")).thenReturn(true);
        when(localWorkspaceRepository.findByUserIdAndPathHash("u-trial", "abcdef0123456789abcdef0123456789"))
                .thenReturn(Optional.empty());
        when(localWorkspaceRepository.save(any(LocalWorkspaceRegistration.class))).thenAnswer(inv -> {
            LocalWorkspaceRegistration r = inv.getArgument(0);
            r.setId("ws-1");
            return r;
        });

        mockMvc.perform(post("/api/v1/workspaces")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.readOnlyMode").value(true));
    }

    @Test
    void webhookRejectsInvalidSignatureWithUnauthorized() throws Exception {
        when(billingService.processVerifiedWebhook(any(), any(), any()))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE", "Webhook signature verification failed."));

        mockMvc.perform(post("/api/v1/billing/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Rmap-Signature", "invalid")
                        .content("{\"eventId\":\"evt_1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_SIGNATURE"));
    }
}
