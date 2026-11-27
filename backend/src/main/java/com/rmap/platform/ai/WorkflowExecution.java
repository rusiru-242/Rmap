package com.rmap.platform.ai;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "ai_workflows")
public class WorkflowExecution {
    @Id
    private String id;
    @Indexed
    private String projectId;
    private String initiatedBy;
    private String status; // QUEUED, RUNNING, AWAITING_APPROVAL, COMPLETED, FAILED, CANCELLED
    private boolean requireHumanCheckpoints;
    private String additionalInstruction;
    private List<AgentStepExecution> steps = new ArrayList<>();
    private long totalPromptTokens;
    private long totalCompletionTokens;
    private double totalEstimatedCostUsd;
    private String errorMessage;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public static class AgentStepExecution {
        private String agentExecutionId;
        private AgentType agentType;
        private String status; // PENDING, RUNNING, AWAITING_APPROVAL, COMPLETED, FAILED, CANCELLED
        private Map<String, Object> validatedOutput;
        private String errorMessage;
        private long promptTokens;
        private long completionTokens;
        private double estimatedCostUsd;
        private long latencyMs;
        private int retryCount;
        private Instant startedAt;
        private Instant completedAt;

        public AgentStepExecution() {}

        public AgentStepExecution(String agentExecutionId, AgentType agentType) {
            this.agentExecutionId = agentExecutionId;
            this.agentType = agentType;
            this.status = "PENDING";
        }

        public String getAgentExecutionId() { return agentExecutionId; }
        public void setAgentExecutionId(String agentExecutionId) { this.agentExecutionId = agentExecutionId; }
        public AgentType getAgentType() { return agentType; }
        public void setAgentType(AgentType agentType) { this.agentType = agentType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Map<String, Object> getValidatedOutput() { return validatedOutput; }
        public void setValidatedOutput(Map<String, Object> validatedOutput) { this.validatedOutput = validatedOutput; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public long getPromptTokens() { return promptTokens; }
        public void setPromptTokens(long promptTokens) { this.promptTokens = promptTokens; }
        public long getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(long completionTokens) { this.completionTokens = completionTokens; }
        public double getEstimatedCostUsd() { return estimatedCostUsd; }
        public void setEstimatedCostUsd(double estimatedCostUsd) { this.estimatedCostUsd = estimatedCostUsd; }
        public long getLatencyMs() { return latencyMs; }
        public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
        public Instant getStartedAt() { return startedAt; }
        public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
        public Instant getCompletedAt() { return completedAt; }
        public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isRequireHumanCheckpoints() { return requireHumanCheckpoints; }
    public void setRequireHumanCheckpoints(boolean requireHumanCheckpoints) { this.requireHumanCheckpoints = requireHumanCheckpoints; }
    public String getAdditionalInstruction() { return additionalInstruction; }
    public void setAdditionalInstruction(String additionalInstruction) { this.additionalInstruction = additionalInstruction; }
    public List<AgentStepExecution> getSteps() { return steps; }
    public void setSteps(List<AgentStepExecution> steps) { this.steps = steps; }
    public long getTotalPromptTokens() { return totalPromptTokens; }
    public void setTotalPromptTokens(long totalPromptTokens) { this.totalPromptTokens = totalPromptTokens; }
    public long getTotalCompletionTokens() { return totalCompletionTokens; }
    public void setTotalCompletionTokens(long totalCompletionTokens) { this.totalCompletionTokens = totalCompletionTokens; }
    public double getTotalEstimatedCostUsd() { return totalEstimatedCostUsd; }
    public void setTotalEstimatedCostUsd(double totalEstimatedCostUsd) { this.totalEstimatedCostUsd = totalEstimatedCostUsd; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
