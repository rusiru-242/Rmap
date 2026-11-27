package com.rmap.platform.spec;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.project.ProjectAuthorizationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AcademicService {
    private final AcademicAssessmentRepository repository;
    private final ProjectAuthorizationService projectAuth;

    public AcademicService(AcademicAssessmentRepository repository, ProjectAuthorizationService projectAuth) {
        this.repository = repository;
        this.projectAuth = projectAuth;
    }

    public AcademicAssessment getOrCreate(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return repository.findByProjectId(projectId).orElseGet(() -> {
            AcademicAssessment a = new AcademicAssessment();
            a.setProjectId(projectId);
            a.setAssignmentType("CUSTOM");
            a.setIntegrityWarnings(defaultIntegrityWarnings());
            return repository.save(a);
        });
    }

    public AcademicAssessment saveAndAudit(String projectId, AuthenticatedUser user, AcademicAssessment incoming) {
        projectAuth.requireEditor(projectId, user);
        AcademicAssessment target = repository.findByProjectId(projectId).orElseGet(() -> {
            AcademicAssessment a = new AcademicAssessment();
            a.setProjectId(projectId);
            return a;
        });

        target.setInstitution(incoming.getInstitution());
        target.setModuleCode(incoming.getModuleCode());
        target.setAssignmentTitle(incoming.getAssignmentTitle());
        target.setAssignmentType(incoming.getAssignmentType() == null ? "CUSTOM" : incoming.getAssignmentType());
        target.setGroupSize(Math.max(1, incoming.getGroupSize()));
        target.setTargetRubricBand(incoming.getTargetRubricBand());
        target.setDeclaredTotalMarks(incoming.getDeclaredTotalMarks() > 0 ? incoming.getDeclaredTotalMarks() : 100.0);
        target.setMandatoryTechnologies(nonNull(incoming.getMandatoryTechnologies()));
        target.setForbiddenTechnologies(nonNull(incoming.getForbiddenTechnologies()));
        target.setRequiredComponents(nonNull(incoming.getRequiredComponents()));
        target.setAiUsageRestrictions(nonNull(incoming.getAiUsageRestrictions()));
        target.setForbiddenPractices(nonNull(incoming.getForbiddenPractices()));
        target.setSubmissionNamingRules(nonNull(incoming.getSubmissionNamingRules()));
        target.setDemoAndVivaRequirements(nonNull(incoming.getDemoAndVivaRequirements()));
        target.setRubricCriteria(incoming.getRubricCriteria() == null ? new ArrayList<>() : incoming.getRubricCriteria());
        target.setMemberResponsibilities(incoming.getMemberResponsibilities() == null ? new ArrayList<>() : incoming.getMemberResponsibilities());

        target.setDetectedInconsistencies(detectInconsistencies(target));
        target.setIntegrityWarnings(defaultIntegrityWarnings());

        return repository.save(target);
    }

    public Map<String, Object> rubricCoverageSummary(String projectId, AuthenticatedUser user) {
        AcademicAssessment a = getOrCreate(projectId, user);
        double criteriaTotal = a.getRubricCriteria().stream().mapToDouble(AcademicAssessment.RubricCriterion::maxMarks).sum();
        long mappedCount = a.getRubricCriteria().stream()
                .filter(c -> c.linkedRequirementIds() != null && !c.linkedRequirementIds().isEmpty())
                .count();
        long verifiedCount = a.getRubricCriteria().stream()
                .filter(c -> "VERIFIED".equalsIgnoreCase(c.verifiedEvidenceStatus()))
                .count();

        return Map.of(
                "declaredTotalMarks", a.getDeclaredTotalMarks(),
                "rubricCriteriaSum", criteriaTotal,
                "totalCriteria", a.getRubricCriteria().size(),
                "plannedMappedCriteria", mappedCount,
                "verifiedEvidenceCriteria", verifiedCount,
                "inconsistencies", a.getDetectedInconsistencies(),
                "disclaimer", "Planned rubric mapping does NOT guarantee academic marks. Verified evidence is collected only from actual implementation and tests."
        );
    }

    public List<String> detectInconsistencies(AcademicAssessment a) {
        List<String> issues = new ArrayList<>();
        if (!a.getRubricCriteria().isEmpty()) {
            double sum = a.getRubricCriteria().stream().mapToDouble(AcademicAssessment.RubricCriterion::maxMarks).sum();
            if (Math.abs(sum - a.getDeclaredTotalMarks()) > 0.01) {
                issues.add(String.format(Locale.ROOT,
                        "Marking scheme inconsistency: rubric criteria sum to %.2f marks, but declared total is %.2f marks. User review required.",
                        sum, a.getDeclaredTotalMarks()));
            }
        }
        for (String mand : a.getMandatoryTechnologies()) {
            for (String forb : a.getForbiddenTechnologies()) {
                if (mand.trim().equalsIgnoreCase(forb.trim())) {
                    issues.add("Conflicting technology rule: '" + mand + "' is listed as both mandatory and forbidden.");
                }
            }
        }
        if (a.getGroupSize() > 1 && a.getMemberResponsibilities().size() < a.getGroupSize()) {
            issues.add("Group size is " + a.getGroupSize() + " but only " + a.getMemberResponsibilities().size() + " individual responsibilities are assigned.");
        }
        return issues;
    }

    private List<String> defaultIntegrityWarnings() {
        return List.of(
                "Rmap never guarantees academic marks or fabricates academic evidence.",
                "Do not use AI to ghostwrite reflections, reports, or essays explicitly required to be in the student's own words.",
                "Maintain a transparent AI Usage Log (ACAD-14) aligned with your institutional policy."
        );
    }

    private List<String> nonNull(List<String> list) {
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }
}
