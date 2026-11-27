package com.rmap.platform.user;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.billing.EntitlementService;
import com.rmap.platform.billing.EntitlementSummary;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserRepository userRepository;
    private final EntitlementService entitlementService;

    public UserController(UserRepository userRepository, EntitlementService entitlementService) {
        this.userRepository = userRepository;
        this.entitlementService = entitlementService;
    }

    public record UserProfileResponse(
            String id,
            String email,
            String fullName,
            Role role,
            UserStatus status,
            boolean emailVerified,
            Instant createdAt,
            Instant lastLoginAt,
            EntitlementSummary entitlement
    ) {}

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 100) String fullName
    ) {}

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
        return ApiResponse.ok(toResponse(user));
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
        user.setFullName(request.fullName().trim());
        user = userRepository.save(user);
        return ApiResponse.ok(toResponse(user));
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                entitlementService.getEntitlement(user.getId())
        );
    }
}
