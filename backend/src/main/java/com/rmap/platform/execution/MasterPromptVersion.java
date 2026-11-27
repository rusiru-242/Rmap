package com.rmap.platform.execution;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "master_prompt_versions")
public class MasterPromptVersion {
    @Id
    private String id;
    @Indexed
    private String projectId;
    private int specVersion;
    private String phaseId;
    private String targetTool; // ANTIGRAVITY, CLAUDE_CODE, OPENAI_CODEX, GITHUB_COPILOT, GENERIC, CUSTOM
    private String promptType; // PHASE_MASTER, TASK, SHARED_FOUNDATION, PER_MEMBER, INTEGRATION, ERROR_FIX, UI_REDESIGN, CODE_REVIEW, SECURITY_REVIEW, TESTING, DEPLOYMENT, MAINTENANCE
    private String assignedMemberEmail;
    private int versionNumber;
    private String promptContent;
    private List<ContextFileEntry> generatedContextFiles = new ArrayList<>();
    private String createdBy;
    @CreatedDate
    private Instant createdAt;

    public record ContextFileEntry(String fileName, String content) {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public int getSpecVersion() { return specVersion; }
    public void setSpecVersion(int specVersion) { this.specVersion = specVersion; }
    public String getPhaseId() { return phaseId; }
    public void setPhaseId(String phaseId) { this.phaseId = phaseId; }
    public String getTargetTool() { return targetTool; }
    public void setTargetTool(String targetTool) { this.targetTool = targetTool; }
    public String getPromptType() { return promptType; }
    public void setPromptType(String promptType) { this.promptType = promptType; }
    public String getAssignedMemberEmail() { return assignedMemberEmail; }
    public void setAssignedMemberEmail(String assignedMemberEmail) { this.assignedMemberEmail = assignedMemberEmail; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }
    public String getPromptContent() { return promptContent; }
    public void setPromptContent(String promptContent) { this.promptContent = promptContent; }
    public List<ContextFileEntry> getGeneratedContextFiles() { return generatedContextFiles; }
    public void setGeneratedContextFiles(List<ContextFileEntry> generatedContextFiles) { this.generatedContextFiles = generatedContextFiles; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
