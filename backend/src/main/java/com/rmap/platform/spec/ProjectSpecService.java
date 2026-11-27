package com.rmap.platform.spec;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.AuditService;
import com.rmap.platform.project.Project;
import com.rmap.platform.project.ProjectAuthorizationService;
import com.rmap.platform.project.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectSpecService {
    private final ProjectSpecRepository specRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAuthorizationService projectAuth;
    private final AuditService auditService;
    private final Clock clock;

    public ProjectSpecService(
            ProjectSpecRepository specRepository,
            ProjectRepository projectRepository,
            ProjectAuthorizationService projectAuth,
            AuditService auditService,
            Clock clock
    ) {
        this.specRepository = specRepository;
        this.projectRepository = projectRepository;
        this.projectAuth = projectAuth;
        this.auditService = auditService;
        this.clock = clock;
    }

    public List<ProjectSpec> listVersions(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return specRepository.findByProjectIdOrderByVersionNumberDesc(projectId);
    }

    public ProjectSpec getLatestOrInitDraft(String projectId, AuthenticatedUser user) {
        Project project = projectAuth.requireMember(projectId, user);
        return specRepository.findFirstByProjectIdOrderByVersionNumberDesc(projectId)
                .orElseGet(() -> createInitialDraft(project));
    }

    public ProjectSpec saveDraft(String projectId, AuthenticatedUser user, ProjectSpec incoming) {
        Project project = projectAuth.requireEditor(projectId, user);
        ProjectSpec latest = specRepository.findFirstByProjectIdOrderByVersionNumberDesc(projectId).orElse(null);

        ProjectSpec target;
        if (latest == null) {
            target = createInitialDraft(project);
        } else if ("APPROVED".equals(latest.getStatus())) {
            // Never silently overwrite an approved specification — fork a new draft version.
            target = new ProjectSpec();
            target.setProjectId(projectId);
            target.setVersionNumber(latest.getVersionNumber() + 1);
            target.setStatus("DRAFT");
        } else {
            target = latest;
        }

        copyEditableFields(incoming, target);
        return specRepository.save(target);
    }

    public ProjectSpec approveVersion(String projectId, int versionNumber, AuthenticatedUser user) {
        Project project = projectAuth.requireEditor(projectId, user);
        ProjectSpec spec = specRepository.findByProjectIdAndVersionNumber(projectId, versionNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SPEC_VERSION_NOT_FOUND", "Specification version not found."));
        if ("APPROVED".equals(spec.getStatus())) {
            return spec;
        }

        List<ProjectSpec> all = specRepository.findByProjectIdOrderByVersionNumberDesc(projectId);
        for (ProjectSpec existing : all) {
            if ("APPROVED".equals(existing.getStatus())) {
                existing.setStatus("SUPERSEDED");
                specRepository.save(existing);
            }
        }

        spec.setStatus("APPROVED");
        spec.setApprovedBy(user.email());
        spec.setApprovedAt(clock.instant());
        spec = specRepository.save(spec);

        project.setActiveSpecVersion(spec.getVersionNumber());
        projectRepository.save(project);

        auditService.record(user.userId(), user.email(), "SPEC_APPROVED", "PROJECT_SPEC", spec.getId(), Map.of(
                "projectId", projectId,
                "versionNumber", spec.getVersionNumber()
        ));
        return spec;
    }

    public Map<String, Object> compareVersions(String projectId, int fromVersion, int toVersion, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        ProjectSpec a = specRepository.findByProjectIdAndVersionNumber(projectId, fromVersion)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SPEC_VERSION_NOT_FOUND", "Version " + fromVersion + " not found."));
        ProjectSpec b = specRepository.findByProjectIdAndVersionNumber(projectId, toVersion)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SPEC_VERSION_NOT_FOUND", "Version " + toVersion + " not found."));

        Set<String> reqA = a.getFunctionalRequirements().stream().map(ProjectSpec.RequirementItem::id).collect(Collectors.toSet());
        Set<String> reqB = b.getFunctionalRequirements().stream().map(ProjectSpec.RequirementItem::id).collect(Collectors.toSet());

        List<String> addedReqs = reqB.stream().filter(id -> !reqA.contains(id)).sorted().toList();
        List<String> removedReqs = reqA.stream().filter(id -> !reqB.contains(id)).sorted().toList();

        List<String> impactedAreas = new ArrayList<>();
        if (!addedReqs.isEmpty() || !removedReqs.isEmpty()) impactedAreas.add("Functional Requirements & Downstream Verification");
        if (!a.getTechnologyStack().equals(b.getTechnologyStack())) impactedAreas.add("Technology Stack & Dependencies");
        if (!a.getDatabaseEntities().equals(b.getDatabaseEntities())) impactedAreas.add("Database Schema");
        if (!a.getApiContracts().equals(b.getApiContracts())) impactedAreas.add("API Contracts");

        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("fromVersion", fromVersion);
        diff.put("toVersion", toVersion);
        diff.put("addedRequirements", addedReqs);
        diff.put("removedRequirements", removedReqs);
        diff.put("architectureChanged", !String.valueOf(a.getArchitectureStyle()).equals(String.valueOf(b.getArchitectureStyle())));
        diff.put("impactedAreas", impactedAreas);
        return diff;
    }

    public ProjectSpec generateArchitectureScaffold(String projectId, AuthenticatedUser user) {
        Project project = projectAuth.requireEditor(projectId, user);
        ProjectSpec spec = getLatestOrInitDraft(projectId, user);
        if ("APPROVED".equals(spec.getStatus())) {
            spec = saveDraft(projectId, user, spec);
        }

        boolean isNotebookMlOnly = isStandaloneMlProject(project);
        if (isNotebookMlOnly) {
            spec.setArchitectureStyle("ML Pipeline Architecture");
            spec.setDatabaseEntities(new ArrayList<>());
            spec.setApiContracts(new ArrayList<>());
            spec.setDiagrams(new ArrayList<>(List.of(
                    new ProjectSpec.DiagramArtifact(
                            "DIAG-001",
                            "ML_PIPELINE",
                            "Reproducible ML Pipeline",
                            "flowchart LR\n  RawData[\"Raw Dataset\"] --> Profiling[\"Deterministic Profiling\"] --> Prep[\"Preprocessing & Split\"] --> Models[\"Baseline & Candidate Models\"] --> Eval[\"Validation & Metrics\"]"
                    )
            )));
        } else {
            if (spec.getArchitectureStyle() == null || spec.getArchitectureStyle().isBlank()) {
                spec.setArchitectureStyle("Modular Monolith");
            }
            spec.setDiagrams(new ArrayList<>(List.of(
                    new ProjectSpec.DiagramArtifact(
                            "DIAG-001",
                            "SYSTEM_CONTEXT",
                            "System Context Diagram",
                            "flowchart LR\n  Users[\"Target Users\"] --> Client[\"Frontend / Client\"] --> Core[\"Application Core\"] --> Data[\"Primary Data Store\"]"
                    ),
                    new ProjectSpec.DiagramArtifact(
                            "DIAG-002",
                            "COMPONENT",
                            "Core Component Architecture",
                            "flowchart TD\n  API[\"API Gateway / Controllers\"] --> Domain[\"Domain Services\"] --> Repo[\"Repositories\"]"
                    )
            )));
        }

        return specRepository.save(spec);
    }

    private boolean isStandaloneMlProject(Project project) {
        if (project.getArchetypes() == null || project.getArchetypes().isEmpty()) return false;
        boolean hasMl = project.getArchetypes().stream().anyMatch(a -> {
            String l = a.toLowerCase(Locale.ROOT);
            return l.contains("machine learning") || l.contains("data science") || l.contains("deep learning");
        });
        boolean hasAppSurface = project.getArchetypes().stream().anyMatch(a -> {
            String l = a.toLowerCase(Locale.ROOT);
            return l.contains("web") || l.contains("mobile") || l.contains("full-stack") || l.contains("api") || l.contains("saas") || l.contains("e-commerce");
        });
        return hasMl && !hasAppSurface;
    }

    private ProjectSpec createInitialDraft(Project project) {
        ProjectSpec spec = new ProjectSpec();
        spec.setProjectId(project.getId());
        spec.setVersionNumber(1);
        spec.setStatus("DRAFT");
        spec.setOverview(project.getDescription());
        spec.setProblemStatement(project.getProblemStatement());
        spec.setObjectives(new ArrayList<>(project.getBusinessObjectives()));
        spec.setDeliverables(new ArrayList<>(project.getDeliverables()));

        List<ProjectSpec.ConstraintItem> constraints = new ArrayList<>();
        int idx = 1;
        for (String rule : project.getMandatoryConstraints()) {
            constraints.add(new ProjectSpec.ConstraintItem(
                    String.format("MUST-%03d", idx++),
                    rule,
                    "MANDATORY_TECH",
                    "Project Creation Input"
            ));
        }
        spec.setMandatoryConstraints(constraints);
        return specRepository.save(spec);
    }

    private void copyEditableFields(ProjectSpec src, ProjectSpec dst) {
        if (src.getOverview() != null) dst.setOverview(src.getOverview());
        if (src.getProblemStatement() != null) dst.setProblemStatement(src.getProblemStatement());
        if (src.getStakeholders() != null) dst.setStakeholders(src.getStakeholders());
        if (src.getObjectives() != null) dst.setObjectives(src.getObjectives());
        if (src.getScope() != null) dst.setScope(src.getScope());
        if (src.getFunctionalRequirements() != null) dst.setFunctionalRequirements(src.getFunctionalRequirements());
        if (src.getNonFunctionalRequirements() != null) dst.setNonFunctionalRequirements(src.getNonFunctionalRequirements());
        if (src.getMandatoryConstraints() != null) dst.setMandatoryConstraints(src.getMandatoryConstraints());
        if (src.getUserRoles() != null) dst.setUserRoles(src.getUserRoles());
        if (src.getBusinessRules() != null) dst.setBusinessRules(src.getBusinessRules());
        if (src.getUserStories() != null) dst.setUserStories(src.getUserStories());
        if (src.getUseCases() != null) dst.setUseCases(src.getUseCases());
        if (src.getArchitectureStyle() != null) dst.setArchitectureStyle(src.getArchitectureStyle());
        if (src.getComponents() != null) dst.setComponents(src.getComponents());
        if (src.getTechnologyStack() != null) dst.setTechnologyStack(src.getTechnologyStack());
        if (src.getDiagrams() != null) dst.setDiagrams(src.getDiagrams());
        if (src.getDatabaseEntities() != null) dst.setDatabaseEntities(src.getDatabaseEntities());
        if (src.getApiContracts() != null) dst.setApiContracts(src.getApiContracts());
        if (src.getUiPages() != null) dst.setUiPages(src.getUiPages());
        if (src.getMobileScreens() != null) dst.setMobileScreens(src.getMobileScreens());
        if (src.getExternalIntegrations() != null) dst.setExternalIntegrations(src.getExternalIntegrations());
        if (src.getAcceptanceCriteria() != null) dst.setAcceptanceCriteria(src.getAcceptanceCriteria());
        if (src.getSecurityRequirements() != null) dst.setSecurityRequirements(src.getSecurityRequirements());
        if (src.getTestingRequirements() != null) dst.setTestingRequirements(src.getTestingRequirements());
        if (src.getMilestones() != null) dst.setMilestones(src.getMilestones());
        if (src.getDeliverables() != null) dst.setDeliverables(src.getDeliverables());
        if (src.getRisks() != null) dst.setRisks(src.getRisks());
        if (src.getAssumptions() != null) dst.setAssumptions(src.getAssumptions());
        if (src.getDecisions() != null) dst.setDecisions(src.getDecisions());
    }
}
