package com.rmap.platform.billing;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.user.Role;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class EntitlementAspect {
    private final EntitlementService entitlementService;

    public EntitlementAspect(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @Before("@annotation(com.rmap.platform.billing.RequiresActiveEntitlement)")
    public void enforceActiveEntitlement() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required.");
        }
        if (user.role() == Role.ADMIN) {
            return;
        }
        if (!entitlementService.hasFullAccess(user.userId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ENTITLEMENT_READ_ONLY",
                    "Your trial or subscription has expired. Your account is currently in read-only mode."
            );
        }
    }
}
