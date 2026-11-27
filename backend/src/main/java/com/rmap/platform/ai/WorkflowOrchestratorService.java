package com.rmap.platform.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.AuditService;
import com.rmap.platform.config.RmapProperties;
import com.rmap.platform.document.ProjectDocument;
import com.rmap.platform.document.ProjectDocumentRepository;
import com.rmap.platform.project.Project;
import com.rmap.platform.project.ProjectAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkflowOrchestratorService {
    private final WorkflowExecutionRepository workflowRepository;
    private final AiUsageRecordRepository usageRepository;
    private final ProjectDocumentRepository documentRepository;
    private final ProjectAuthorizationService projectAuth;
    private final LlmProvider llmProvider;
    private final Map<AgentType, InternalAgent> agentMap;
    private final AuditService auditService;
    private final RmapProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public WorkflowOrchestratorService(
            WorkflowExecutionRepository workflowRepository,
            AiUsageRecordRepository usageRepository,
            ProjectDocumentRepository documentRepository,
            ProjectAuthorizationService projectAuth,
            LlmProvider llmProvider,
            List<InternalAgent> agents,
            AuditService auditService,
            RmapProperties properties,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.workflowRepository = workflowRepository;
        this.usageRepository = usageRepository;
        this.documentRepository = documentRepository;
        this.projectAuth = projectAuth;
        this.llmProvider = llmProvider;
        this.agentMap = agents.stream().collect(Collectors.toMap(InternalAgent::type, Function.identity()));
        this.auditService = auditService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public Map<String, Object> getAiStatus() {
        return Map.of(
                "provider", llmProvider.providerName(),
                "model", llmProvider.activeModel(),
                "configured", llmProvider.isConfigured(),
                "agents", agentMap.values().stream()
                        .map(a -> Map.of("type", a.type().name(), "responsibility", a.responsibilitySummary()))
                        .toList()
        );
    }

    public WorkflowExecution startWorkflow(
            String projectId,
            AuthenticatedUser user,
            List<AgentType> requestedAgents,
            boolean requireHumanCheckpoints,
            String additionalInstruction
    ) {
        Project project = projectAuth.requireEditor(projectId, user);
        enforceDailyQuota(user.userId());

        List<AgentType> pipeline = (requestedAgents == null || requestedAgents.isEmpty())
                ? List.of(AgentType.DISCOVERY, AgentType.ARCHITECT, AgentType.ENGINEERING, AgentType.QUALITY_DELIVERY)
                : requestedAgents.stream().distinct().toList();

        WorkflowExecution wf = new WorkflowExecution();
        wf.setProjectId(projectId);
        wf.setInitiatedBy(user.userId());
        wf.setStatus("RUNNING");
        wf.setRequireHumanCheckpoints(requireHumanCheckpoints);
        wf.setAdditionalInstruction(additionalInstruction);
        for (AgentType type : pipeline) {
            wf.getSteps().add(new WorkflowExecution.AgentStepExecution(UUID.randomUUID().toString(), type));
        }
        wf = workflowRepository.save(wf);

        return executeFromPending(wf, project, user);
    }

    public WorkflowExecution approveCheckpoint(String projectId, String workflowId, AuthenticatedUser user) {
        Project project = projectAuth.requireEditor(projectId, user);
        WorkflowExecution wf = requireWorkflow(projectId, workflowId);
        if (!"AWAITING_APPROVAL".equals(wf.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WORKFLOW_NOT_AWAITING_APPROVAL", "Workflow is not paused at an approval checkpoint.");
        }
        for (var step : wf.getSteps()) {
            if ("AWAITING_APPROVAL".equals(step.getStatus())) {
                step.setStatus("COMPLETED");
            }
        }
        wf.setStatus("RUNNING");
        wf = workflowRepository.save(wf);
        return executeFromPending(wf, project, user);
    }

    public WorkflowExecution retryFailed(String projectId, String workflowId, AuthenticatedUser user) {
        Project project = projectAuth.requireEditor(projectId, user);
        WorkflowExecution wf = requireWorkflow(projectId, workflowId);
        if (!"FAILED".equals(wf.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WORKFLOW_NOT_FAILED", "Only failed workflows can be retried.");
        }
        for (var step : wf.getSteps()) {
            if ("FAILED".equals(step.getStatus())) {
                step.setStatus("PENDING");
                step.setErrorMessage(null);
                step.setRetryCount(step.getRetryCount() + 1);
            }
        }
        wf.setStatus("RUNNING");
        wf.setErrorMessage(null);
        wf = workflowRepository.save(wf);
        return executeFromPending(wf, project, user);
    }

    public WorkflowExecution cancel(String projectId, String workflowId, AuthenticatedUser user) {
        projectAuth.requireEditor(projectId, user);
        WorkflowExecution wf = requireWorkflow(projectId, workflowId);
        wf.setStatus("CANCELLED");
        for (var step : wf.getSteps()) {
            if ("PENDING".equals(step.getStatus()) || "RUNNING".equals(step.getStatus()) || "AWAITING_APPROVAL".equals(step.getStatus())) {
                step.setStatus("CANCELLED");
            }
        }
        auditService.record(user.userId(), user.email(), "AI_WORKFLOW_CANCELLED", "AI_WORKFLOW", workflowId, Map.of("projectId", projectId));
        return workflowRepository.save(wf);
    }

    public List<WorkflowExecution> listWorkflows(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return workflowRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public List<AiUsageRecord> listUsage(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return usageRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    private WorkflowExecution executeFromPending(WorkflowExecution wf, Project project, AuthenticatedUser user) {
        List<ProjectDocument> docs = documentRepository.findByProjectIdOrderByCreatedAtDesc(project.getId());
        Map<AgentType, JsonNode> priorOutputs = new EnumMap<>(AgentType.class);
        for (var step : wf.getSteps()) {
            if ("COMPLETED".equals(step.getStatus()) && step.getValidatedOutput() != null) {
                priorOutputs.put(step.getAgentType(), objectMapper.valueToTree(step.getValidatedOutput()));
            }
        }

        for (int i = 0; i < wf.getSteps().size(); i++) {
            var step = wf.getSteps().get(i);
            if (!"PENDING".equals(step.getStatus())) continue;

            InternalAgent agent = agentMap.get(step.getAgentType());
            if (agent == null) {
                step.setStatus("FAILED");
                step.setErrorMessage("No agent registered for " + step.getAgentType());
                wf.setStatus("FAILED");
                wf.setErrorMessage(step.getErrorMessage());
                return workflowRepository.save(wf);
            }

            step.setStatus("RUNNING");
            step.setStartedAt(clock.instant());
            workflowRepository.save(wf);

            try {
                String prompt = agent.buildUserPrompt(new InternalAgent.AgentContext(
                        project, docs, priorOutputs, wf.getAdditionalInstruction()
                ));
                LlmProvider.LlmResponse response = llmProvider.generateJson(
                        new LlmProvider.LlmRequest(agent.systemInstruction(), prompt, 0.2, 4096)
                );

                JsonNode parsed = objectMapper.readTree(response.rawJson());
                agent.validateSchema(parsed);

                Map<String, Object> asMap = objectMapper.convertValue(parsed, new TypeReference<>() {});
                step.setValidatedOutput(asMap);
                step.setPromptTokens(response.promptTokens());
                step.setCompletionTokens(response.completionTokens());
                step.setEstimatedCostUsd(response.estimatedCostUsd());
                step.setLatencyMs(response.latencyMs());
                step.setCompletedAt(clock.instant());

                wf.setTotalPromptTokens(wf.getTotalPromptTokens() + response.promptTokens());
                wf.setTotalCompletionTokens(wf.getTotalCompletionTokens() + response.completionTokens());
                wf.setTotalEstimatedCostUsd(wf.getTotalEstimatedCostUsd() + response.estimatedCostUsd());

                usageRepository.save(new AiUsageRecord(
                        user.userId(), project.getId(), wf.getId(), step.getAgentType().name(),
                        response.model(), response.promptTokens(), response.completionTokens(),
                        response.estimatedCostUsd(), response.latencyMs(), clock.instant()
                ));

                priorOutputs.put(step.getAgentType(), parsed);

                boolean hasRemainingSteps = i < wf.getSteps().size() - 1;
                if (wf.isRequireHumanCheckpoints() && hasRemainingSteps
                        && (step.getAgentType() == AgentType.DISCOVERY || step.getAgentType() == AgentType.ARCHITECT)) {
                    step.setStatus("AWAITING_APPROVAL");
                    wf.setStatus("AWAITING_APPROVAL");
                    return workflowRepository.save(wf);
                }

                step.setStatus("COMPLETED");
            } catch (Exception ex) {
                step.setStatus("FAILED");
                step.setErrorMessage(ex.getMessage());
                step.setCompletedAt(clock.instant());
                wf.setStatus("FAILED");
                wf.setErrorMessage(ex.getMessage());
                auditService.record(user.userId(), user.email(), "AI_WORKFLOW_FAILED", "AI_WORKFLOW", wf.getId(), Map.of(
                        "agent", step.getAgentType().name(),
                        "error", String.valueOf(ex.getMessage())
                ));
                return workflowRepository.save(wf);
            }
        }

        wf.setStatus("COMPLETED");
        auditService.record(user.userId(), user.email(), "AI_WORKFLOW_COMPLETED", "AI_WORKFLOW", wf.getId(), Map.of(
                "projectId", project.getId(),
                "totalTokens", wf.getTotalPromptTokens() + wf.getTotalCompletionTokens()
        ));
        return workflowRepository.save(wf);
    }

    private WorkflowExecution requireWorkflow(String projectId, String workflowId) {
        return workflowRepository.findById(workflowId)
                .filter(w -> w.getProjectId().equals(projectId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKFLOW_NOT_FOUND", "Workflow execution not found."));
    }

    private void enforceDailyQuota(String userId) {
        long quota = properties.ai() != null && properties.ai().dailyTokenQuota() > 0
                ? properties.ai().dailyTokenQuota() : 250_000L;
        Instant since = clock.instant().minus(24, ChronoUnit.HOURS);
        long used = usageRepository.findByUserIdAndCreatedAtAfter(userId, since).stream()
                .mapToLong(r -> r.getPromptTokens() + r.getCompletionTokens())
                .sum();
        if (used >= quota) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI_DAILY_QUOTA_EXCEEDED",
                    "Daily AI token quota reached (" + used + "/" + quota + ").");
        }
    }
}
