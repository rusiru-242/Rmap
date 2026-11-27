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

@Document(collection = "phase_plans")
public class PhasePlan {
    @Id
    private String id;
    @Indexed(unique = true)
    private String projectId;
    private int specVersion;
    private String planningMode; // QUICK, STANDARD, CUSTOM
    private List<PhaseItem> phases = new ArrayList<>();
    @Version
    private Long version;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public record PhaseItem(
            String phaseId, // PH-001
            String title,
            String objective,
            String scope,
            List<String> prerequisites,
            List<String> dependencies,
            List<String> requirementIds,
            List<String> assignedMembers,
            List<String> expectedArtifacts,
            String implementationInstructions,
            List<String> acceptanceCriteria,
            List<String> verificationCommands,
            String estimatedEffort,
            String status, // NOT_STARTED, PLANNED, IN_PROGRESS, IMPLEMENTED, VERIFIED, BLOCKED
            List<String> completionEvidenceIds,
            List<String> risksAndBlockers
    ) {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public int getSpecVersion() { return specVersion; }
    public void setSpecVersion(int specVersion) { this.specVersion = specVersion; }
    public String getPlanningMode() { return planningMode; }
    public void setPlanningMode(String planningMode) { this.planningMode = planningMode; }
    public List<PhaseItem> getPhases() { return phases; }
    public void setPhases(List<PhaseItem> phases) { this.phases = phases; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
