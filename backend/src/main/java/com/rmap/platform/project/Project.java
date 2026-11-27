package com.rmap.platform.project;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "projects")
public class Project {
    @Id
    private String id;
    @Indexed
    private String ownerId;
    @Indexed
    private String name;
    private String description;
    private String problemStatement;
    private String targetUsers;
    private List<String> businessObjectives = new ArrayList<>();
    @Indexed
    private ProjectMode mode = ProjectMode.INDIVIDUAL;
    private List<String> archetypes = new ArrayList<>();
    private List<String> platforms = new ArrayList<>();
    private int teamSize = 1;
    private String experienceLevel;
    private String budget;
    private String deadline;
    private List<String> technologyPreferences = new ArrayList<>();
    private List<String> mandatoryConstraints = new ArrayList<>();
    private List<String> deliverables = new ArrayList<>();
    private List<String> inputSources = new ArrayList<>();
    private String localWorkspaceId;
    @Indexed
    private ProjectStatus status = ProjectStatus.ACTIVE;
    private List<ProjectMember> members = new ArrayList<>();
    private List<ProjectInvitation> invitations = new ArrayList<>();
    private List<ProjectActivity> activityHistory = new ArrayList<>();
    private int activeSpecVersion = 0;
    @Version
    private Long version;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public record ProjectMember(
            String userId,
            String email,
            ProjectRole role,
            String responsibility,
            Instant joinedAt
    ) {}

    public record ProjectInvitation(
            String id,
            String email,
            ProjectRole role,
            String responsibility,
            String status,
            Instant invitedAt
    ) {}

    public record ProjectActivity(
            String actorId,
            String actorEmail,
            String action,
            String detail,
            Instant timestamp
    ) {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getProblemStatement() { return problemStatement; }
    public void setProblemStatement(String problemStatement) { this.problemStatement = problemStatement; }
    public String getTargetUsers() { return targetUsers; }
    public void setTargetUsers(String targetUsers) { this.targetUsers = targetUsers; }
    public List<String> getBusinessObjectives() { return businessObjectives; }
    public void setBusinessObjectives(List<String> businessObjectives) { this.businessObjectives = businessObjectives; }
    public ProjectMode getMode() { return mode; }
    public void setMode(ProjectMode mode) { this.mode = mode; }
    public List<String> getArchetypes() { return archetypes; }
    public void setArchetypes(List<String> archetypes) { this.archetypes = archetypes; }
    public List<String> getPlatforms() { return platforms; }
    public void setPlatforms(List<String> platforms) { this.platforms = platforms; }
    public int getTeamSize() { return teamSize; }
    public void setTeamSize(int teamSize) { this.teamSize = teamSize; }
    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }
    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public List<String> getTechnologyPreferences() { return technologyPreferences; }
    public void setTechnologyPreferences(List<String> technologyPreferences) { this.technologyPreferences = technologyPreferences; }
    public List<String> getMandatoryConstraints() { return mandatoryConstraints; }
    public void setMandatoryConstraints(List<String> mandatoryConstraints) { this.mandatoryConstraints = mandatoryConstraints; }
    public List<String> getDeliverables() { return deliverables; }
    public void setDeliverables(List<String> deliverables) { this.deliverables = deliverables; }
    public List<String> getInputSources() { return inputSources; }
    public void setInputSources(List<String> inputSources) { this.inputSources = inputSources; }
    public String getLocalWorkspaceId() { return localWorkspaceId; }
    public void setLocalWorkspaceId(String localWorkspaceId) { this.localWorkspaceId = localWorkspaceId; }
    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }
    public List<ProjectMember> getMembers() { return members; }
    public void setMembers(List<ProjectMember> members) { this.members = members; }
    public List<ProjectInvitation> getInvitations() { return invitations; }
    public void setInvitations(List<ProjectInvitation> invitations) { this.invitations = invitations; }
    public List<ProjectActivity> getActivityHistory() { return activityHistory; }
    public void setActivityHistory(List<ProjectActivity> activityHistory) { this.activityHistory = activityHistory; }
    public int getActiveSpecVersion() { return activeSpecVersion; }
    public void setActiveSpecVersion(int activeSpecVersion) { this.activeSpecVersion = activeSpecVersion; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
