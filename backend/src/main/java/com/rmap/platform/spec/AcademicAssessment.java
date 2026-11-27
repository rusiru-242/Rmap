package com.rmap.platform.spec;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "academic_assessments")
public class AcademicAssessment {
    @Id
    private String id;
    @Indexed(unique = true)
    private String projectId;
    private String institution;
    private String moduleCode;
    private String assignmentTitle;
    private String assignmentType; // INDIVIDUAL, GROUP, FULL_STACK, AGENTIC_AI, ML, HACKATHON, OOP, CUSTOM
    private int groupSize = 1;
    private String targetRubricBand; // e.g., Distinction, First Class, A
    private double declaredTotalMarks = 100.0;
    private List<String> mandatoryTechnologies = new ArrayList<>();
    private List<String> forbiddenTechnologies = new ArrayList<>();
    private List<String> requiredComponents = new ArrayList<>();
    private List<String> aiUsageRestrictions = new ArrayList<>();
    private List<String> forbiddenPractices = new ArrayList<>();
    private List<String> submissionNamingRules = new ArrayList<>();
    private List<String> demoAndVivaRequirements = new ArrayList<>();
    private List<RubricCriterion> rubricCriteria = new ArrayList<>();
    private List<MemberResponsibility> memberResponsibilities = new ArrayList<>();
    private List<String> detectedInconsistencies = new ArrayList<>();
    private List<String> integrityWarnings = new ArrayList<>();
    @LastModifiedDate
    private Instant updatedAt;

    public record RubricCriterion(
            String id,                 // RUB-001
            String title,
            double maxMarks,
            String performanceBandSummary,
            String sourceRef,
            List<String> linkedRequirementIds,
            String plannedCoverageStatus,   // PLANNED, PARTIAL, UNMAPPED
            String verifiedEvidenceStatus   // NOT_VERIFIED, VERIFIED_IN_PHASE_3
    ) {}

    public record MemberResponsibility(
            String memberEmail,
            String individualComponent,
            List<String> ownedRequirementIds
    ) {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
    public String getAssignmentTitle() { return assignmentTitle; }
    public void setAssignmentTitle(String assignmentTitle) { this.assignmentTitle = assignmentTitle; }
    public String getAssignmentType() { return assignmentType; }
    public void setAssignmentType(String assignmentType) { this.assignmentType = assignmentType; }
    public int getGroupSize() { return groupSize; }
    public void setGroupSize(int groupSize) { this.groupSize = groupSize; }
    public String getTargetRubricBand() { return targetRubricBand; }
    public void setTargetRubricBand(String targetRubricBand) { this.targetRubricBand = targetRubricBand; }
    public double getDeclaredTotalMarks() { return declaredTotalMarks; }
    public void setDeclaredTotalMarks(double declaredTotalMarks) { this.declaredTotalMarks = declaredTotalMarks; }
    public List<String> getMandatoryTechnologies() { return mandatoryTechnologies; }
    public void setMandatoryTechnologies(List<String> mandatoryTechnologies) { this.mandatoryTechnologies = mandatoryTechnologies; }
    public List<String> getForbiddenTechnologies() { return forbiddenTechnologies; }
    public void setForbiddenTechnologies(List<String> forbiddenTechnologies) { this.forbiddenTechnologies = forbiddenTechnologies; }
    public List<String> getRequiredComponents() { return requiredComponents; }
    public void setRequiredComponents(List<String> requiredComponents) { this.requiredComponents = requiredComponents; }
    public List<String> getAiUsageRestrictions() { return aiUsageRestrictions; }
    public void setAiUsageRestrictions(List<String> aiUsageRestrictions) { this.aiUsageRestrictions = aiUsageRestrictions; }
    public List<String> getForbiddenPractices() { return forbiddenPractices; }
    public void setForbiddenPractices(List<String> forbiddenPractices) { this.forbiddenPractices = forbiddenPractices; }
    public List<String> getSubmissionNamingRules() { return submissionNamingRules; }
    public void setSubmissionNamingRules(List<String> submissionNamingRules) { this.submissionNamingRules = submissionNamingRules; }
    public List<String> getDemoAndVivaRequirements() { return demoAndVivaRequirements; }
    public void setDemoAndVivaRequirements(List<String> demoAndVivaRequirements) { this.demoAndVivaRequirements = demoAndVivaRequirements; }
    public List<RubricCriterion> getRubricCriteria() { return rubricCriteria; }
    public void setRubricCriteria(List<RubricCriterion> rubricCriteria) { this.rubricCriteria = rubricCriteria; }
    public List<MemberResponsibility> getMemberResponsibilities() { return memberResponsibilities; }
    public void setMemberResponsibilities(List<MemberResponsibility> memberResponsibilities) { this.memberResponsibilities = memberResponsibilities; }
    public List<String> getDetectedInconsistencies() { return detectedInconsistencies; }
    public void setDetectedInconsistencies(List<String> detectedInconsistencies) { this.detectedInconsistencies = detectedInconsistencies; }
    public List<String> getIntegrityWarnings() { return integrityWarnings; }
    public void setIntegrityWarnings(List<String> integrityWarnings) { this.integrityWarnings = integrityWarnings; }
    public Instant getUpdatedAt() { return updatedAt; }
}
