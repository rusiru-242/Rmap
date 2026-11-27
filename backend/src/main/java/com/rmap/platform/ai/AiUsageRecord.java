package com.rmap.platform.ai;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "ai_usage_records")
public class AiUsageRecord {
    @Id
    private String id;
    @Indexed
    private String userId;
    @Indexed
    private String projectId;
    private String workflowId;
    private String agentType;
    private String model;
    private long promptTokens;
    private long completionTokens;
    private double estimatedCostUsd;
    private long latencyMs;
    @Indexed
    private Instant createdAt;

    public AiUsageRecord() {}

    public AiUsageRecord(
            String userId,
            String projectId,
            String workflowId,
            String agentType,
            String model,
            long promptTokens,
            long completionTokens,
            double estimatedCostUsd,
            long latencyMs,
            Instant createdAt
    ) {
        this.userId = userId;
        this.projectId = projectId;
        this.workflowId = workflowId;
        this.agentType = agentType;
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.estimatedCostUsd = estimatedCostUsd;
        this.latencyMs = latencyMs;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getProjectId() { return projectId; }
    public String getWorkflowId() { return workflowId; }
    public String getAgentType() { return agentType; }
    public String getModel() { return model; }
    public long getPromptTokens() { return promptTokens; }
    public long getCompletionTokens() { return completionTokens; }
    public double getEstimatedCostUsd() { return estimatedCostUsd; }
    public long getLatencyMs() { return latencyMs; }
    public Instant getCreatedAt() { return createdAt; }
}
