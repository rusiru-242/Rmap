package com.rmap.platform.project;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.billing.RequiresActiveEntitlement;
import com.rmap.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresActiveEntitlement
    public ApiResponse<Project> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ProjectDtos.CreateProjectRequest request
    ) {
        return ApiResponse.ok(projectService.create(user, request));
    }

    @GetMapping
    public ApiResponse<List<Project>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProjectMode mode,
            @RequestParam(required = false) ProjectStatus status
    ) {
        return ApiResponse.ok(projectService.list(user, q, mode, status));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<Project> get(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(projectService.get(projectId, user));
    }

    @PatchMapping("/{projectId}")
    @RequiresActiveEntitlement
    public ApiResponse<Project> update(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ProjectDtos.UpdateProjectRequest request
    ) {
        return ApiResponse.ok(projectService.update(projectId, user, request));
    }

    @PostMapping("/{projectId}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresActiveEntitlement
    public ApiResponse<Project> duplicate(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(projectService.duplicate(projectId, user));
    }

    @PostMapping("/{projectId}/archive")
    @RequiresActiveEntitlement
    public ApiResponse<Project> archive(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(projectService.setArchiveState(projectId, user, true));
    }

    @PostMapping("/{projectId}/restore")
    @RequiresActiveEntitlement
    public ApiResponse<Project> restore(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(projectService.setArchiveState(projectId, user, false));
    }

    @DeleteMapping("/{projectId}")
    @RequiresActiveEntitlement
    public ApiResponse<Map<String, String>> delete(
            @PathVariable String projectId,
            @RequestParam String confirmName,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        projectService.delete(projectId, user, confirmName);
        return ApiResponse.ok(Map.of("deleted", projectId));
    }

    @PostMapping("/{projectId}/invitations")
    @RequiresActiveEntitlement
    public ApiResponse<Project> invite(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ProjectDtos.InviteMemberRequest request
    ) {
        return ApiResponse.ok(projectService.inviteMember(projectId, user, request));
    }

    @PostMapping("/{projectId}/workspace")
    @RequiresActiveEntitlement
    public ApiResponse<Project> linkWorkspace(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ProjectDtos.LinkWorkspaceRequest request
    ) {
        return ApiResponse.ok(projectService.linkLocalWorkspace(projectId, user, request.localWorkspaceId()));
    }
}
