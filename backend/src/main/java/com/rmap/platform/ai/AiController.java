package com.rmap.platform.ai;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.billing.RequiresActiveEntitlement;
import com.rmap.platform.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AiController {
    private final WorkflowOrchestratorService orchestrator;

    public AiController(WorkflowOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    public record RunWorkflowRequest(
            List<AgentType> agents,
            boolean requireHumanCheckpoints,
            String additionalInstruction
    ) {}

    @GetMapping("/ai/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(orchestrator.getAiStatus());
    }

    @PostMapping("/projects/{projectId}/ai/workflows")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresActiveEntitlement
    public ApiResponse<WorkflowExecution> startWorkflow(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody(required = false) RunWorkflowRequest request
    ) {
        RunWorkflowRequest req = request == null ? new RunWorkflowRequest(null, true, "") : request;
        return ApiResponse.ok(orchestrator.startWorkflow(
                projectId, user, req.agents(), req.requireHumanCheckpoints(), req.additionalInstruction()
        ));
    }

    @GetMapping("/projects/{projectId}/ai/workflows")
    public ApiResponse<List<WorkflowExecution>> listWorkflows(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(orchestrator.listWorkflows(projectId, user));
    }

    @PostMapping("/projects/{projectId}/ai/workflows/{workflowId}/approve")
    @RequiresActiveEntitlement
    public ApiResponse<WorkflowExecution> approve(
            @PathVariable String projectId,
            @PathVariable String workflowId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(orchestrator.approveCheckpoint(projectId, workflowId, user));
    }

    @PostMapping("/projects/{projectId}/ai/workflows/{workflowId}/retry")
    @RequiresActiveEntitlement
    public ApiResponse<WorkflowExecution> retry(
            @PathVariable String projectId,
            @PathVariable String workflowId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(orchestrator.retryFailed(projectId, workflowId, user));
    }

    @PostMapping("/projects/{projectId}/ai/workflows/{workflowId}/cancel")
    @RequiresActiveEntitlement
    public ApiResponse<WorkflowExecution> cancel(
            @PathVariable String projectId,
            @PathVariable String workflowId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(orchestrator.cancel(projectId, workflowId, user));
    }

    @GetMapping("/projects/{projectId}/ai/usage")
    public ApiResponse<List<AiUsageRecord>> usage(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(orchestrator.listUsage(projectId, user));
    }
}
