package com.rmap.platform.spec;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "project_specs")
@CompoundIndex(name = "idx_spec_project_version", def = "{'projectId': 1, 'versionNumber': -1}", unique = true)
public class ProjectSpec {
    @Id
    private String id;
    @Indexed
    private String projectId;
    private int versionNumber;
    private String status; // DRAFT, IN_REVIEW, APPROVED, SUPERSEDED
    private String approvedBy;
    private Instant approvedAt;

    private String overview;
    private String problemStatement;
    private List<String> stakeholders = new ArrayList<>();
    private List<String> objectives = new ArrayList<>();
    private String scope;

    private List<RequirementItem> functionalRequirements = new ArrayList<>();
    private List<RequirementItem> nonFunctionalRequirements = new ArrayList<>();
    private List<ConstraintItem> mandatoryConstraints = new ArrayList<>();
    private List<RolePermissionItem> userRoles = new ArrayList<>();
    private List<String> businessRules = new ArrayList<>();
    private List<String> userStories = new ArrayList<>();
    private List<String> useCases = new ArrayList<>();

    private String architectureStyle;
    private List<ComponentItem> components = new ArrayList<>();
    private List<TechSelection> technologyStack = new ArrayList<>();
    private List<DiagramArtifact> diagrams = new ArrayList<>();
    private List<DatabaseEntityItem> databaseEntities = new ArrayList<>();
    private List<ApiContractItem> apiContracts = new ArrayList<>();
    private List<UiScreenItem> uiPages = new ArrayList<>();
    private List<UiScreenItem> mobileScreens = new ArrayList<>();
    private List<String> externalIntegrations = new ArrayList<>();

    private List<String> acceptanceCriteria = new ArrayList<>();
    private List<String> securityRequirements = new ArrayList<>();
    private List<String> testingRequirements = new ArrayList<>();
    private List<String> milestones = new ArrayList<>();
    private List<String> deliverables = new ArrayList<>();
    private List<String> risks = new ArrayList<>();
    private List<String> assumptions = new ArrayList<>();
    private List<AdrDecision> decisions = new ArrayList<>();

    @Version
    private Long optimisticLockVersion;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public record RequirementItem(
            String id,          // REQ-001, NFR-001
            String title,
            String description,
            String priority,
            String sourceRef,
            boolean aiRecommended,
            boolean confirmed,
            List<String> dependsOn
    ) {}

    public record ConstraintItem(
            String id,          // MUST-001
            String rule,
            String type,        // MANDATORY_TECH, FORBIDDEN_TECH, DEADLINE, BUDGET, ACADEMIC_RULE, SECURITY
            String sourceRef
    ) {}

    public record RolePermissionItem(
            String id,          // ROLE-001
            String roleName,
            List<String> permissions
    ) {}

    public record ComponentItem(
            String id,          // COMP-001
            String name,
            String responsibility,
            List<String> dependencies
    ) {}

    public record TechSelection(
            String category,
            String name,
            String rationale,
            String tradeoffs,
            boolean mandatoryOverride
    ) {}

    public record DiagramArtifact(
            String id,
            String diagramType, // SYSTEM_CONTEXT, COMPONENT, ERD, SEQUENCE, DATA_FLOW, ML_PIPELINE
            String title,
            String mermaidCode
    ) {}

    public record DatabaseEntityItem(
            String id,          // ENT-001
            String name,
            List<String> fields,
            List<String> relationships
    ) {}

    public record ApiContractItem(
            String id,          // API-001
            String method,
            String path,
            String summary,
            String authRole
    ) {}

    public record UiScreenItem(
            String id,          // PAGE-001
            String name,
            String route,
            String purpose
    ) {}

    public record AdrDecision(
            String id,
            String title,
            String decision,
            String rationale
    ) {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public String getProblemStatement() { return problemStatement; }
    public void setProblemStatement(String problemStatement) { this.problemStatement = problemStatement; }
    public List<String> getStakeholders() { return stakeholders; }
    public void setStakeholders(List<String> stakeholders) { this.stakeholders = stakeholders; }
    public List<String> getObjectives() { return objectives; }
    public void setObjectives(List<String> objectives) { this.objectives = objectives; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public List<RequirementItem> getFunctionalRequirements() { return functionalRequirements; }
    public void setFunctionalRequirements(List<RequirementItem> functionalRequirements) { this.functionalRequirements = functionalRequirements; }
    public List<RequirementItem> getNonFunctionalRequirements() { return nonFunctionalRequirements; }
    public void setNonFunctionalRequirements(List<RequirementItem> nonFunctionalRequirements) { this.nonFunctionalRequirements = nonFunctionalRequirements; }
    public List<ConstraintItem> getMandatoryConstraints() { return mandatoryConstraints; }
    public void setMandatoryConstraints(List<ConstraintItem> mandatoryConstraints) { this.mandatoryConstraints = mandatoryConstraints; }
    public List<RolePermissionItem> getUserRoles() { return userRoles; }
    public void setUserRoles(List<RolePermissionItem> userRoles) { this.userRoles = userRoles; }
    public List<String> getBusinessRules() { return businessRules; }
    public void setBusinessRules(List<String> businessRules) { this.businessRules = businessRules; }
    public List<String> getUserStories() { return userStories; }
    public void setUserStories(List<String> userStories) { this.userStories = userStories; }
    public List<String> getUseCases() { return useCases; }
    public void setUseCases(List<String> useCases) { this.useCases = useCases; }
    public String getArchitectureStyle() { return architectureStyle; }
    public void setArchitectureStyle(String architectureStyle) { this.architectureStyle = architectureStyle; }
    public List<ComponentItem> getComponents() { return components; }
    public void setComponents(List<ComponentItem> components) { this.components = components; }
    public List<TechSelection> getTechnologyStack() { return technologyStack; }
    public void setTechnologyStack(List<TechSelection> technologyStack) { this.technologyStack = technologyStack; }
    public List<DiagramArtifact> getDiagrams() { return diagrams; }
    public void setDiagrams(List<DiagramArtifact> diagrams) { this.diagrams = diagrams; }
    public List<DatabaseEntityItem> getDatabaseEntities() { return databaseEntities; }
    public void setDatabaseEntities(List<DatabaseEntityItem> databaseEntities) { this.databaseEntities = databaseEntities; }
    public List<ApiContractItem> getApiContracts() { return apiContracts; }
    public void setApiContracts(List<ApiContractItem> apiContracts) { this.apiContracts = apiContracts; }
    public List<UiScreenItem> getUiPages() { return uiPages; }
    public void setUiPages(List<UiScreenItem> uiPages) { this.uiPages = uiPages; }
    public List<UiScreenItem> getMobileScreens() { return mobileScreens; }
    public void setMobileScreens(List<UiScreenItem> mobileScreens) { this.mobileScreens = mobileScreens; }
    public List<String> getExternalIntegrations() { return externalIntegrations; }
    public void setExternalIntegrations(List<String> externalIntegrations) { this.externalIntegrations = externalIntegrations; }
    public List<String> getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(List<String> acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }
    public List<String> getSecurityRequirements() { return securityRequirements; }
    public void setSecurityRequirements(List<String> securityRequirements) { this.securityRequirements = securityRequirements; }
    public List<String> getTestingRequirements() { return testingRequirements; }
    public void setTestingRequirements(List<String> testingRequirements) { this.testingRequirements = testingRequirements; }
    public List<String> getMilestones() { return milestones; }
    public void setMilestones(List<String> milestones) { this.milestones = milestones; }
    public List<String> getDeliverables() { return deliverables; }
    public void setDeliverables(List<String> deliverables) { this.deliverables = deliverables; }
    public List<String> getRisks() { return risks; }
    public void setRisks(List<String> risks) { this.risks = risks; }
    public List<String> getAssumptions() { return assumptions; }
    public void setAssumptions(List<String> assumptions) { this.assumptions = assumptions; }
    public List<AdrDecision> getDecisions() { return decisions; }
    public void setDecisions(List<AdrDecision> decisions) { this.decisions = decisions; }
    public Long getOptimisticLockVersion() { return optimisticLockVersion; }
    public void setOptimisticLockVersion(Long optimisticLockVersion) { this.optimisticLockVersion = optimisticLockVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
