package com.rmap.platform.workspace;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.billing.RequiresActiveEntitlement;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.ApiResponse;
import com.rmap.platform.common.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {
    private final LocalWorkspaceRepository repository;
    private final AuditService auditService;

    public WorkspaceController(LocalWorkspaceRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    public record RegisterWorkspaceRequest(
            @NotBlank @Size(max = 120) String displayName,
            @NotBlank @Size(min = 16, max = 128) String pathHash,
            List<String> detectedStacks,
            @Min(0) int indexedFileCount,
            @Min(0) int excludedSensitiveFileCount
    ) {}

    @GetMapping
    public ApiResponse<List<LocalWorkspaceRegistration>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(repository.findByUserIdOrderByUpdatedAtDesc(user.userId()));
    }

    @PostMapping
    @RequiresActiveEntitlement
    public ApiResponse<LocalWorkspaceRegistration> register(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody RegisterWorkspaceRequest request
    ) {
        LocalWorkspaceRegistration entity = repository.findByUserIdAndPathHash(user.userId(), request.pathHash())
                .orElseGet(LocalWorkspaceRegistration::new);
        entity.setUserId(user.userId());
        entity.setDisplayName(request.displayName().trim());
        entity.setPathHash(request.pathHash().trim());
        entity.setDetectedStacks(request.detectedStacks() == null ? List.of() : request.detectedStacks());
        entity.setIndexedFileCount(request.indexedFileCount());
        entity.setExcludedSensitiveFileCount(request.excludedSensitiveFileCount());
        entity.setReadOnlyMode(true);
        entity = repository.save(entity);

        auditService.record(user.userId(), user.email(), "WORKSPACE_REGISTERED", "WORKSPACE", entity.getId(), Map.of(
                "displayName", entity.getDisplayName(),
                "indexedFileCount", entity.getIndexedFileCount(),
                "excludedSensitiveFileCount", entity.getExcludedSensitiveFileCount()
        ));

        return ApiResponse.ok(entity);
    }

    @DeleteMapping("/{id}")
    @RequiresActiveEntitlement
    public ApiResponse<Map<String, String>> remove(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String id
    ) {
        LocalWorkspaceRegistration entity = repository.findById(id)
                .filter(w -> w.getUserId().equals(user.userId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKSPACE_NOT_FOUND", "Workspace registration not found."));
        repository.delete(entity);
        return ApiResponse.ok(Map.of("deleted", id));
    }
}
