package com.rmap.platform.execution;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "project_tasks")
public class ProjectTask {
    @Id
    private String id;
    @Indexed
    private String projectId;
    @Indexed
    private String phaseId;
    private String taskCode; // TASK-001
    private String title;
    private String description;
    private String status; // NOT_STARTED, PLANNED, IN_PROGRESS, IMPLEMENTED, VERIFIED, BLOCKED
    private String priority; // LOW, MEDIUM, HIGH, CRITICAL
    private String assignedMemberEmail;
    private List<String> requirementIds = new ArrayList<>();
    private List<String> dependsOnTaskCodes = new ArrayList<>();
    private List<String> acceptanceCriteria = new ArrayList<>();
    private List<String> subtasks = new ArrayList<>();
    private List<String> comments = new ArrayList<>();
    private List<String> evidenceIds = new ArrayList<>();
    private String agentExecutionId;
    private String milestone;
    private String dueDate;
    @Version
    private Long version;
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
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getAssignedMemberEmail() { return assignedMemberEmail; }
    public void setAssignedMemberEmail(String assignedMemberEmail) { this.assignedMemberEmail = assignedMemberEmail; }
    public List<String> getRequirementIds() { return requirementIds; }
    public void setRequirementIds(List<String> requirementIds) { this.requirementIds = requirementIds; }
    public List<String> getDependsOnTaskCodes() { return dependsOnTaskCodes; }
    public void setDependsOnTaskCodes(List<String> dependsOnTaskCodes) { this.dependsOnTaskCodes = dependsOnTaskCodes; }
    public List<String> getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(List<String> acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }
    public List<String> getSubtasks() { return subtasks; }
    public void setSubtasks(List<String> subtasks) { this.subtasks = subtasks; }
    public List<String> getComments() { return comments; }
    public void setComments(List<String> comments) { this.comments = comments; }
    public List<String> getEvidenceIds() { return evidenceIds; }
    public void setEvidenceIds(List<String> evidenceIds) { this.evidenceIds = evidenceIds; }
    public String getAgentExecutionId() { return agentExecutionId; }
    public void setAgentExecutionId(String agentExecutionId) { this.agentExecutionId = agentExecutionId; }
    public String getMilestone() { return milestone; }
    public void setMilestone(String milestone) { this.milestone = milestone; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
