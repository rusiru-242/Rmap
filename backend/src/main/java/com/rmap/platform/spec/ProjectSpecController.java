package com.rmap.platform.spec;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.billing.RequiresActiveEntitlement;
import com.rmap.platform.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/specs")
public class ProjectSpecController {
    private final ProjectSpecService specService;

    public ProjectSpecController(ProjectSpecService specService) {
        this.specService = specService;
    }

    @GetMapping
    public ApiResponse<List<ProjectSpec>> list(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(specService.listVersions(projectId, user));
    }

    @GetMapping("/latest")
    public ApiResponse<ProjectSpec> latest(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(specService.getLatestOrInitDraft(projectId, user));
    }

    @PutMapping("/draft")
    @RequiresActiveEntitlement
    public ApiResponse<ProjectSpec> saveDraft(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody ProjectSpec spec
    ) {
        return ApiResponse.ok(specService.saveDraft(projectId, user, spec));
    }

    @PostMapping("/{versionNumber}/approve")
    @RequiresActiveEntitlement
    public ApiResponse<ProjectSpec> approve(
            @PathVariable String projectId,
            @PathVariable int versionNumber,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(specService.approveVersion(projectId, versionNumber, user));
    }

    @GetMapping("/compare")
    public ApiResponse<Map<String, Object>> compare(
            @PathVariable String projectId,
            @RequestParam int from,
            @RequestParam int to,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(specService.compareVersions(projectId, from, to, user));
    }

    @PostMapping("/architecture/generate")
    @RequiresActiveEntitlement
    public ApiResponse<ProjectSpec> generateArchitecture(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(specService.generateArchitectureScaffold(projectId, user));
    }
}
