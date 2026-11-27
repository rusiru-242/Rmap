package com.rmap.platform;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.AuditService;
import com.rmap.platform.delivery.DeliveryService;
import com.rmap.platform.delivery.DeploymentPlan;
import com.rmap.platform.delivery.DeploymentPlanRepository;
import com.rmap.platform.delivery.DesignArtifactRepository;
import com.rmap.platform.delivery.GeneratedDocument;
import com.rmap.platform.delivery.GeneratedDocumentRepository;
import com.rmap.platform.delivery.MlExperiment;
import com.rmap.platform.delivery.MlExperimentRepository;
import com.rmap.platform.execution.AiUsageLogEntry;
import com.rmap.platform.execution.AiUsageLogEntryRepository;
import com.rmap.platform.execution.CodingAgentExecutionRepository;
import com.rmap.platform.execution.ErrorIncident;
import com.rmap.platform.execution.ErrorIncidentRepository;
import com.rmap.platform.execution.ExecutionService;
import com.rmap.platform.execution.MasterPromptVersion;
import com.rmap.platform.execution.MasterPromptVersionRepository;
import com.rmap.platform.execution.PhasePlan;
import com.rmap.platform.execution.PhasePlanRepository;
import com.rmap.platform.execution.ProjectTask;
import com.rmap.platform.execution.ProjectTaskRepository;
import com.rmap.platform.execution.VerificationEvidence;
import com.rmap.platform.execution.VerificationEvidenceRepository;
import com.rmap.platform.project.Project;
import com.rmap.platform.project.ProjectAuthorizationService;
import com.rmap.platform.project.ProjectMode;
import com.rmap.platform.project.ProjectRepository;
import com.rmap.platform.spec.AcademicAssessment;
import com.rmap.platform.spec.AcademicAssessmentRepository;
import com.rmap.platform.spec.ProjectSpec;
import com.rmap.platform.spec.ProjectSpecRepository;
import com.rmap.platform.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Phase3ExecutionAndDeliveryTest {

    @Mock private PhasePlanRepository phasePlanRepository;
    @Mock private ProjectTaskRepository taskRepository;
    @Mock private MasterPromptVersionRepository promptRepository;
    @Mock private CodingAgentExecutionRepository agentExecutionRepository;
    @Mock private VerificationEvidenceRepository evidenceRepository;
    @Mock private ErrorIncidentRepository errorRepository;
    @Mock private AiUsageLogEntryRepository aiUsageLogRepository;
    @Mock private ProjectSpecRepository specRepository;
    @Mock private AcademicAssessmentRepository academicRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private AuditService auditService;

    @Mock private DesignArtifactRepository designRepository;
    @Mock private GeneratedDocumentRepository docRepository;
    @Mock private MlExperimentRepository mlRepository;
    @Mock private DeploymentPlanRepository deploymentRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-27T11:30:00Z"), ZoneOffset.UTC);
    private final AuthenticatedUser user = new AuthenticatedUser("u-1", "dev@example.com", Role.USER);

    private ExecutionService executionService;
    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        ProjectAuthorizationService projectAuth = new ProjectAuthorizationService(projectRepository);
        executionService = new ExecutionService(
                phasePlanRepository, taskRepository, promptRepository, agentExecutionRepository,
                evidenceRepository, errorRepository, aiUsageLogRepository, specRepository,
                academicRepository, projectAuth, auditService, clock
        );
        deliveryService = new DeliveryService(
                designRepository, docRepository, mlRepository, deploymentRepository,
                specRepository, academicRepository, projectAuth, executionService
        );
    }

    @Test
    void phasePlannerSupportsQuickStandardAndRejectsCircularDependencies() {
        Project project = sampleProject("p-1", ProjectMode.STARTUP, List.of("Full-Stack", "E-Commerce"));
        when(projectRepository.findById("p-1")).thenReturn(Optional.of(project));
        when(specRepository.findFirstByProjectIdOrderByVersionNumberDesc("p-1")).thenReturn(Optional.empty());
        when(phasePlanRepository.findByProjectId("p-1")).thenReturn(Optional.empty());
        when(phasePlanRepository.save(any(PhasePlan.class))).thenAnswer(inv -> inv.getArgument(0));

        PhasePlan quick = executionService.generateOrReplanPhases("p-1", user, "QUICK", null);
        assertThat(quick.getPhases().size()).isBetween(1, 4);

        PhasePlan standard = executionService.generateOrReplanPhases("p-1", user, "STANDARD", null);
        assertThat(standard.getPhases().size()).isBetween(5, 10);

        List<PhasePlan.PhaseItem> cyclic = List.of(
                new PhasePlan.PhaseItem("PH-001", "A", "", "", List.of(), List.of("PH-002"), List.of(), List.of(), List.of(), "", List.of(), List.of(), "", "PLANNED", List.of(), List.of()),
                new PhasePlan.PhaseItem("PH-002", "B", "", "", List.of(), List.of("PH-001"), List.of(), List.of(), List.of(), "", List.of(), List.of(), "", "PLANNED", List.of(), List.of())
        );
        assertThatThrownBy(() -> executionService.validateNoCircularDependencies(cyclic))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Circular phase dependency");
    }

    @Test
    void masterPromptEnforcesAcad03MandatoryGateAndRecordsAiUsageLog() {
        Project project = sampleProject("p-acad", ProjectMode.ACADEMIC, List.of("Full-Stack", "Agentic AI"));
        AcademicAssessment incomplete = new AcademicAssessment();
        incomplete.setProjectId("p-acad");
        incomplete.setGroupSize(1);
        incomplete.setRubricCriteria(List.of(
                new AcademicAssessment.RubricCriterion("RUB-001", "Core System", 50, "Distinction", "brief p.2", List.of(), "UNMAPPED", "NOT_VERIFIED")
        ));

        when(projectRepository.findById("p-acad")).thenReturn(Optional.of(project));
        when(specRepository.findFirstByProjectIdOrderByVersionNumberDesc("p-acad")).thenReturn(Optional.empty());
        when(phasePlanRepository.findByProjectId("p-acad")).thenReturn(Optional.empty());
        when(taskRepository.findByProjectIdOrderByCreatedAtAsc("p-acad")).thenReturn(List.of());
        when(academicRepository.findByProjectId("p-acad")).thenReturn(Optional.of(incomplete));

        assertThatThrownBy(() -> executionService.generateMasterPrompt("p-acad", user, "PH-001", "CLAUDE_CODE", "PHASE_MASTER", null, true))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("ACAD-03 violation");

        AcademicAssessment complete = new AcademicAssessment();
        complete.setProjectId("p-acad");
        complete.setGroupSize(1);
        complete.setRubricCriteria(List.of(
                new AcademicAssessment.RubricCriterion("RUB-001", "Core System", 50, "Distinction", "brief p.2", List.of("REQ-001"), "PLANNED", "NOT_VERIFIED")
        ));
        when(academicRepository.findByProjectId("p-acad")).thenReturn(Optional.of(complete));
        when(promptRepository.save(any(MasterPromptVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        MasterPromptVersion pv = executionService.generateMasterPrompt("p-acad", user, "PH-001", "CLAUDE_CODE", "PHASE_MASTER", "dev@example.com", true);
        assertThat(pv.getPromptContent()).contains("## 1. Agent Role").contains("## 19. Next-Phase Context");
        assertThat(pv.getGeneratedContextFiles().stream().map(MasterPromptVersion.ContextFileEntry::fileName).toList())
                .contains("AGENTS.md", "CLAUDE.md");
        verify(aiUsageLogRepository).save(any(AiUsageLogEntry.class));
    }

    @Test
    void verificationDistinguishesClaimsFromRealTestsAndInvalidatesStaleOnChangeRequest() {
        Project project = sampleProject("p-ver", ProjectMode.CLIENT, List.of("Web Application"));
        when(projectRepository.findById("p-ver")).thenReturn(Optional.of(project));
        when(specRepository.findFirstByProjectIdOrderByVersionNumberDesc("p-ver")).thenReturn(Optional.empty());
        when(evidenceRepository.save(any(VerificationEvidence.class))).thenAnswer(inv -> inv.getArgument(0));

        VerificationEvidence claim = new VerificationEvidence();
        claim.setRequirementId("REQ-001");
        claim.setEvidenceStrength("AGENT_CLAIM");
        assertThat(executionService.recordEvidence("p-ver", user, claim).getVerificationStatus()).isEqualTo("CLAIM_UNVERIFIED");

        VerificationEvidence testPass = new VerificationEvidence();
        testPass.setRequirementId("REQ-001");
        testPass.setEvidenceStrength("TEST_PASS");
        testPass.setActualResult("19/19 passed");
        VerificationEvidence saved = executionService.recordEvidence("p-ver", user, testPass);
        assertThat(saved.getVerificationStatus()).isEqualTo("VERIFIED");

        when(evidenceRepository.findByProjectIdAndRequirementId("p-ver", "REQ-001")).thenReturn(List.of(saved));
        Map<String, Object> impact = deliveryService.processClientChangeRequest("p-ver", user, "REQ-001", "Update checkout tax rule");
        assertThat(impact.get("staleEvidenceInvalidated")).isEqualTo(1);
        assertThat(saved.getVerificationStatus()).isEqualTo("STALE");

        ProjectTask task = new ProjectTask();
        task.setId("t-1");
        task.setProjectId("p-ver");
        task.setRequirementIds(List.of("REQ-002"));
        when(taskRepository.findById("t-1")).thenReturn(Optional.of(task));
        when(evidenceRepository.findByProjectIdAndRequirementId("p-ver", "REQ-002")).thenReturn(List.of());
        assertThatThrownBy(() -> executionService.updateTaskStatus("p-ver", "t-1", user, "VERIFIED"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("verification evidence");
    }

    @Test
    void errorFixStudioEnforcesBoundedRepairAttempts() {
        Project project = sampleProject("p-err", ProjectMode.INDIVIDUAL, List.of("Web Application"));
        when(projectRepository.findById("p-err")).thenReturn(Optional.of(project));
        when(errorRepository.save(any(ErrorIncident.class))).thenAnswer(inv -> {
            ErrorIncident e = inv.getArgument(0);
            if (e.getId() == null) e.setId("err-1");
            return e;
        });

        ErrorIncident inc = executionService.diagnoseError("p-err", user, "Build failed", "COMPILER", "Cannot find symbol in AuthController.java");
        assertThat(inc.getCategory()).isEqualTo("COMPILATION");
        assertThat(inc.getAffectedComponent()).contains("Java");

        when(errorRepository.findById("err-1")).thenReturn(Optional.of(inc));
        executionService.recordRepairAttempt("p-err", "err-1", user, "MANUAL", "Attempt 1", false);
        executionService.recordRepairAttempt("p-err", "err-1", user, "MANUAL", "Attempt 2", false);
        executionService.recordRepairAttempt("p-err", "err-1", user, "MANUAL", "Attempt 3", false);

        assertThatThrownBy(() -> executionService.recordRepairAttempt("p-err", "err-1", user, "MANUAL", "Attempt 4", false))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Bounded repair limit");
    }

    @Test
    void documentationExportsValidPdfAndDocxAndDeploymentGenerates20StepsAndMlRequiresRealSource() {
        Project project = sampleProject("p-del", ProjectMode.INDIVIDUAL, List.of("Machine Learning"));
        when(projectRepository.findById("p-del")).thenReturn(Optional.of(project));
        when(specRepository.findFirstByProjectIdOrderByVersionNumberDesc("p-del")).thenReturn(Optional.empty());
        when(phasePlanRepository.findByProjectId("p-del")).thenReturn(Optional.empty());
        when(taskRepository.findByProjectIdOrderByCreatedAtAsc("p-del")).thenReturn(List.of());
        when(evidenceRepository.findByProjectIdOrderByRecordedAtDesc("p-del")).thenReturn(List.of());
        when(docRepository.save(any(GeneratedDocument.class))).thenAnswer(inv -> {
            GeneratedDocument d = inv.getArgument(0);
            d.setId("doc-1");
            return d;
        });

        GeneratedDocument srs = deliveryService.generateDocumentation("p-del", user, "SRS");
        when(docRepository.findById("doc-1")).thenReturn(Optional.of(srs));

        byte[] pdfBytes = deliveryService.exportDocumentBytes("p-del", "doc-1", user, "PDF");
        assertThat(new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        byte[] docxBytes = deliveryService.exportDocumentBytes("p-del", "doc-1", user, "DOCX");
        assertThat(docxBytes[0]).isEqualTo((byte) 'P');
        assertThat(docxBytes[1]).isEqualTo((byte) 'K');

        when(deploymentRepository.save(any(DeploymentPlan.class))).thenAnswer(inv -> inv.getArgument(0));
        DeploymentPlan deploy = deliveryService.generateDeploymentPlan("p-del", user, "Render", "WEB_SERVICE", "backend", "./mvnw package", "java -jar app.jar");
        assertThat(deploy.getClickByClickManualSteps()).hasSize(20);

        MlExperiment exp = new MlExperiment();
        exp.setId("exp-1");
        exp.setProjectId("p-del");
        exp.setModelFamily("LogisticRegression");
        when(mlRepository.findById("exp-1")).thenReturn(Optional.of(exp));
        when(mlRepository.save(any(MlExperiment.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> deliveryService.importRealExperimentResults("p-del", "exp-1", user, "", Map.of("f1", 0.85), ""))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("resultSource");

        MlExperiment imported = deliveryService.importRealExperimentResults("p-del", "exp-1", user, "notebooks/baseline.ipynb (seed=42)", Map.of("macro_f1", 0.81), "5-fold CV");
        assertThat(imported.getStatus()).isEqualTo("RESULTS_IMPORTED");
        assertThat(imported.getImportedMetrics().get("macro_f1")).isEqualTo(0.81);
    }

    private Project sampleProject(String id, ProjectMode mode, List<String> archetypes) {
        Project p = new Project();
        p.setId(id);
        p.setOwnerId("u-1");
        p.setName("Test Project " + id);
        p.setDescription("Synthetic test description");
        p.setMode(mode);
        p.setArchetypes(archetypes);
        return p;
    }
}
