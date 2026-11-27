package com.rmap.platform.execution;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "coding_agent_executions")
public class CodingAgentExecution {
    @Id
    private String id;
    @Indexed
    private String projectId;
    private String phaseId;
    private String taskCode;
    private String targetAgent; // ANTIGRAVITY, CLAUDE_CODE, OPENAI_CODEX, GITHUB_COPILOT, GENERIC, CUSTOM
    private String integrationMode; // MANUAL, ASSISTED, AUTOMATED
    private String state; // PENDING, WAITING_FOR_APPROVAL, RUNNING, COMPLETED, FAILED, CANCELLED, BLOCKED, VERIFICATION_REQUIRED, VERIFIED
    private String promptVersionId;
    private String importedReport;
    private List<String> modifiedFiles = new ArrayList<>();
    private String verificationNotes;
    private String createdBy;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getPhaseId() { return phaseId; }
    public void setPhaseId(String phaseId) { this.phaseId = phaseId; }
    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }
    public String getTargetAgent() { return targetAgent; }
    public void setTargetAgent(String targetAgent) { this.targetAgent = targetAgent; }
    public String getIntegrationMode() { return integrationMode; }
    public void setIntegrationMode(String integrationMode) { this.integrationMode = integrationMode; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPromptVersionId() { return promptVersionId; }
    public void setPromptVersionId(String promptVersionId) { this.promptVersionId = promptVersionId; }
    public String getImportedReport() { return importedReport; }
    public void setImportedReport(String importedReport) { this.importedReport = importedReport; }
    public List<String> getModifiedFiles() { return modifiedFiles; }
    public void setModifiedFiles(List<String> modifiedFiles) { this.modifiedFiles = modifiedFiles; }
    public String getVerificationNotes() { return verificationNotes; }
    public void setVerificationNotes(String verificationNotes) { this.verificationNotes = verificationNotes; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
