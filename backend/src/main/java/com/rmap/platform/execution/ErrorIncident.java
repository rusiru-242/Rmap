package com.rmap.platform.execution;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "error_incidents")
public class ErrorIncident {
    @Id
    private String id;
    @Indexed
    private String projectId;
    private String title;
    private String sourceType; // TERMINAL, COMPILER, STACK_TRACE, API_ERROR, TEST_FAILURE, BROWSER_CONSOLE
    private String category; // COMPILATION, DEPENDENCY, CONFIGURATION, RUNTIME, DATABASE, API_INTEGRATION, AUTH, FRONTEND, PORT_CONFLICT, NETWORK, DEPLOYMENT, TEST_FAILURE, PERFORMANCE
    private String rawLogExcerpt;
    private String affectedComponent;
    private String likelyRootCause;
    private String repairPrompt;
    private String rollbackGuidance;
    private String status; // OPEN, DIAGNOSED, REPAIR_ATTEMPTED, VERIFIED_FIXED, BLOCKED
    private int repairAttempts;
    private int maxRepairAttempts = 3;
    private List<RepairAttemptRecord> attemptHistory = new ArrayList<>();
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public record RepairAttemptRecord(
            int attemptNumber,
            String fixMode,
            String summary,
            String verificationResult,
            Instant timestamp
    ) {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRawLogExcerpt() { return rawLogExcerpt; }
    public void setRawLogExcerpt(String rawLogExcerpt) { this.rawLogExcerpt = rawLogExcerpt; }
    public String getAffectedComponent() { return affectedComponent; }
    public void setAffectedComponent(String affectedComponent) { this.affectedComponent = affectedComponent; }
    public String getLikelyRootCause() { return likelyRootCause; }
    public void setLikelyRootCause(String likelyRootCause) { this.likelyRootCause = likelyRootCause; }
    public String getRepairPrompt() { return repairPrompt; }
    public void setRepairPrompt(String repairPrompt) { this.repairPrompt = repairPrompt; }
    public String getRollbackGuidance() { return rollbackGuidance; }
    public void setRollbackGuidance(String rollbackGuidance) { this.rollbackGuidance = rollbackGuidance; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getRepairAttempts() { return repairAttempts; }
    public void setRepairAttempts(int repairAttempts) { this.repairAttempts = repairAttempts; }
    public int getMaxRepairAttempts() { return maxRepairAttempts; }
    public void setMaxRepairAttempts(int maxRepairAttempts) { this.maxRepairAttempts = maxRepairAttempts; }
    public List<RepairAttemptRecord> getAttemptHistory() { return attemptHistory; }
    public void setAttemptHistory(List<RepairAttemptRecord> attemptHistory) { this.attemptHistory = attemptHistory; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
