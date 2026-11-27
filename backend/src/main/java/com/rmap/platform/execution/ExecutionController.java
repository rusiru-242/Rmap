package com.rmap.platform.execution;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.billing.RequiresActiveEntitlement;
import com.rmap.platform.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/execution")
public class ExecutionController {
    private final ExecutionService service;

    public ExecutionController(ExecutionService service) {
        this.service = service;
    }

    public record GeneratePhasePlanRequest(String mode, Integer customPhaseCount) {}
    public record GeneratePromptRequest(String phaseId, String targetTool, String promptType, String assignedMemberEmail, boolean enforceMandatoryGate) {}
    public record UpdateTaskStatusRequest(String status) {}
    public record TransitionAgentRequest(String state, String importedReport, List<String> modifiedFiles) {}
    public record DiagnoseErrorRequest(String title, String sourceType, String rawLog) {}
    public record RecordRepairRequest(String fixMode, String summary, boolean verificationPassed) {}

    @PostMapping("/phases/generate")
    @RequiresActiveEntitlement
    public ApiResponse<PhasePlan> generatePhases(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody(required = false) GeneratePhasePlanRequest req
    ) {
        String mode = req == null ? "STANDARD" : req.mode();
        Integer count = req == null ? null : req.customPhaseCount();
        return ApiResponse.ok(service.generateOrReplanPhases(projectId, user, mode, count));
    }

    @PutMapping("/phases")
    @RequiresActiveEntitlement
    public ApiResponse<PhasePlan> saveCustomPlan(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody PhasePlan plan
    ) {
        return ApiResponse.ok(service.saveCustomPlan(projectId, user, plan));
    }

    @GetMapping("/phases")
    public ApiResponse<PhasePlan> getPhases(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(service.getPhasePlan(projectId, user));
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresActiveEntitlement
    public ApiResponse<ProjectTask> createTask(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody ProjectTask task
    ) {
        return ApiResponse.ok(service.createTask(projectId, user, task));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<ProjectTask>> listTasks(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(service.listTasks(projectId, user));
    }

    @PatchMapping("/tasks/{taskId}/status")
    @RequiresActiveEntitlement
    public ApiResponse<ProjectTask> updateTaskStatus(
            @PathVariable String projectId,
            @PathVariable String taskId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody UpdateTaskStatusRequest req
    ) {
        return ApiResponse.ok(service.updateTaskStatus(projectId, taskId, user, req.status()));
    }

    @PostMapping("/prompts")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresActiveEntitlement
    public ApiResponse<MasterPromptVersion> generatePrompt(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody GeneratePromptRequest req
    ) {
        return ApiResponse.ok(service.generateMasterPrompt(
                projectId, user, req.phaseId(), req.targetTool(), req.promptType(), req.assignedMemberEmail(), req.enforceMandatoryGate()
        ));
    }

    @GetMapping("/prompts")
    public ApiResponse<List<MasterPromptVersion>> listPrompts(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(service.listPrompts(projectId, user));
    }

    @GetMapping("/ai-usage-logs")
    public ApiResponse<List<AiUsageLogEntry>> listAiUsageLogs(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(service.listAiUsageLogs(projectId, user));
    }

    @GetMapping("/coding-agents/adapters")
    public ApiResponse<List<Map<String, Object>>> adapters() {
        return ApiResponse.ok(service.listCodingAgentAdapters());
    }

    @PostMapping("/coding-agents/executions")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresActiveEntitlement
    public ApiResponse<CodingAgentExecution> createAgentExecution(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody CodingAgentExecution req
    ) {
        return ApiResponse.ok(service.createAgentExecution(projectId, user, req));
    }

    @PatchMapping("/coding-agents/executions/{execId}")
    @RequiresActiveEntitlement
    public ApiResponse<CodingAgentExecution> transitionAgentExecution(
            @PathVariable String projectId,
            @PathVariable String execId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody TransitionAgentRequest req
    ) {
        return ApiResponse.ok(service.transitionAgentExecution(projectId, execId, user, req.state(), req.importedReport(), req.modifiedFiles()));
    }

    @GetMapping("/coding-agents/executions")
    public ApiResponse<List<CodingAgentExecution>> listAgentExecutions(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(service.listAgentExecutions(projectId, user));
    }

    @PostMapping("/evidence")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresActiveEntitlement
    public ApiResponse<VerificationEvidence> recordEvidence(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody VerificationEvidence req
    ) {
        return ApiResponse.ok(service.recordEvidence(projectId, user, req));
    }

    @PostMapping("/evidence/{requirementId}/invalidate")
    @RequiresActiveEntitlement
    public ApiResponse<Map<String, Object>> invalidateEvidence(
            @PathVariable String projectId,
            @PathVariable String requirementId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        int count = service.invalidateEvidenceForRequirement(projectId, user, requirementId);
        return ApiResponse.ok(Map.of("requirementId", requirementId, "invalidatedCount", count));
    }

    @GetMapping("/traceability")
    public ApiResponse<Map<String, Object>> traceability(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(service.getTraceabilityMatrix(projectId, user));
    }

    @PostMapping("/errors/diagnose")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresActiveEntitlement
    public ApiResponse<ErrorIncident> diagnose(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody DiagnoseErrorRequest req
    ) {
        return ApiResponse.ok(service.diagnoseError(projectId, user, req.title(), req.sourceType(), req.rawLog()));
    }

    @PostMapping("/errors/{incidentId}/repairs")
    @RequiresActiveEntitlement
    public ApiResponse<ErrorIncident> recordRepair(
            @PathVariable String projectId,
            @PathVariable String incidentId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody RecordRepairRequest req
    ) {
        return ApiResponse.ok(service.recordRepairAttempt(projectId, incidentId, user, req.fixMode(), req.summary(), req.verificationPassed()));
    }

    @GetMapping("/errors")
    public ApiResponse<List<ErrorIncident>> listErrors(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(service.listErrorIncidents(projectId, user));
    }
}
