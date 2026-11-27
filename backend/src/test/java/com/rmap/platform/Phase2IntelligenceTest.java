package com.rmap.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rmap.platform.ai.AgentType;
import com.rmap.platform.ai.AiUsageRecordRepository;
import com.rmap.platform.ai.GeminiLlmProvider;
import com.rmap.platform.ai.LlmProvider;
import com.rmap.platform.ai.SpecializedAgents;
import com.rmap.platform.ai.WorkflowExecution;
import com.rmap.platform.ai.WorkflowExecutionRepository;
import com.rmap.platform.ai.WorkflowOrchestratorService;
import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.chat.ChatConversation;
import com.rmap.platform.chat.ChatConversationRepository;
import com.rmap.platform.chat.ProjectChatService;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.AuditService;
import com.rmap.platform.config.RmapProperties;
import com.rmap.platform.document.ProjectDocumentRepository;
import com.rmap.platform.document.SafeDocumentParser;
import com.rmap.platform.project.Project;
import com.rmap.platform.project.ProjectAuthorizationService;
import com.rmap.platform.project.ProjectMode;
import com.rmap.platform.project.ProjectRepository;
import com.rmap.platform.spec.AcademicAssessment;
import com.rmap.platform.spec.AcademicAssessmentRepository;
import com.rmap.platform.spec.AcademicService;
import com.rmap.platform.spec.DatasetIntelligenceService;
import com.rmap.platform.spec.DatasetProfile;
import com.rmap.platform.spec.DatasetProfileRepository;
import com.rmap.platform.spec.ProjectSpec;
import com.rmap.platform.spec.ProjectSpecRepository;
import com.rmap.platform.spec.ProjectSpecService;
import com.rmap.platform.spec.TechnologyCatalogRepository;
import com.rmap.platform.spec.TechnologyCatalogService;
import com.rmap.platform.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Phase2IntelligenceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectSpecRepository specRepository;
    @Mock private ProjectDocumentRepository documentRepository;
    @Mock private WorkflowExecutionRepository workflowRepository;
    @Mock private AiUsageRecordRepository usageRepository;
    @Mock private AcademicAssessmentRepository academicRepository;
    @Mock private DatasetProfileRepository datasetRepository;
    @Mock private ChatConversationRepository chatRepository;
    @Mock private TechnologyCatalogRepository techCatalogRepository;
    @Mock private AuditService auditService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-27T11:00:00Z"), ZoneOffset.UTC);
    private final AuthenticatedUser userA = new AuthenticatedUser("user-a", "a@example.com", Role.USER);
    private final AuthenticatedUser userB = new AuthenticatedUser("user-b", "b@example.com", Role.USER);

    private RmapProperties testProps() {
        return new RmapProperties(
                new RmapProperties.SecurityProperties("test-secret-key-with-more-than-thirty-two-bytes-length", 900, 604800, 600, List.of()),
                new RmapProperties.MailProperties("no-reply@rmap.local"),
                new RmapProperties.BillingProperties(72, "unconfigured", ""),
                new RmapProperties.BootstrapProperties("", ""),
                new RmapProperties.AiProperties("", "gemini-2.5-flash", "https://generativelanguage.googleapis.com/v1beta", 45, 2, 250000),
                new RmapProperties.UploadProperties(15728640, 500, 52428800)
        );
    }

    @Test
    void documentParserBlocksPathTraversalAndZipSlipAndRedactsSecrets() throws Exception {
        SafeDocumentParser parser = new SafeDocumentParser(testProps());

        assertThatThrownBy(() -> parser.parse("../evil.txt", "hello".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid characters");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("../../etc/passwd"));
            zos.write("malicious".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        assertThatThrownBy(() -> parser.parse("evil.zip", baos.toByteArray()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("path traversal");

        var parsed = parser.parse("notes.md", "API_KEY=sk-live-secret123\nNormal text".getBytes(StandardCharsets.UTF_8));
        assertThat(parsed.chunks().get(0).text()).contains("API_KEY=[REDACTED]");
        assertThat(parsed.chunks().get(0).text()).doesNotContain("sk-live-secret123");
        assertThat(parsed.securityWarnings()).isNotEmpty();
    }

    @Test
    void crossProjectAuthorizationBlocksUnauthorizedUser() {
        Project project = new Project();
        project.setId("proj-1");
        project.setOwnerId("user-a");
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));

        ProjectAuthorizationService auth = new ProjectAuthorizationService(projectRepository);
        assertThat(auth.requireMember("proj-1", userA).getId()).isEqualTo("proj-1");
        assertThatThrownBy(() -> auth.requireMember("proj-1", userB))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("do not have access");
    }

    @Test
    void geminiProviderWithoutApiKeyReturnsTruthfulConfigurationRequiredError() {
        GeminiLlmProvider provider = new GeminiLlmProvider(testProps(), objectMapper);
        assertThat(provider.isConfigured()).isFalse();
        assertThatThrownBy(() -> provider.generateJson(new LlmProvider.LlmRequest("sys", "user", 0.2, 512)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("GEMINI_API_KEY");
    }

    @Test
    void workflowOrchestratorEnforcesSchemaValidationCheckpointsAndFailedRecovery() {
        Project project = new Project();
        project.setId("proj-ecom");
        project.setOwnerId("user-a");
        project.setName("Scenario A E-Commerce");
        project.setMode(ProjectMode.STARTUP);
        project.setArchetypes(List.of("Full-Stack", "E-Commerce"));

        when(projectRepository.findById("proj-ecom")).thenReturn(Optional.of(project));
        when(documentRepository.findByProjectIdOrderByCreatedAtDesc("proj-ecom")).thenReturn(List.of());
        when(usageRepository.findByUserIdAndCreatedAtAfter(any(), any())).thenReturn(List.of());
        when(workflowRepository.save(any(WorkflowExecution.class))).thenAnswer(inv -> {
            WorkflowExecution w = inv.getArgument(0);
            if (w.getId() == null) w.setId("wf-1");
            return w;
        });

        String validDiscoveryJson = """
                {
                  "summary": "E-commerce platform",
                  "functionalRequirements": [{"id":"REQ-001","title":"Catalog","description":"Browse products","sourceRef":"User Prompt"}],
                  "nonFunctionalRequirements": [{"id":"NFR-001","category":"Security","description":"TLS + RBAC"}],
                  "mandatoryConstraints": [],
                  "conflicts": [],
                  "clarificationQuestions": ["Which payment region?"]
                }
                """;
        String invalidArchitectJson = """
                {"architectureStyle": "Modular Monolith"}
                """;
        String validArchitectJson = """
                {
                  "architectureStyle": "Modular Monolith",
                  "technologyStack": [{"category":"Backend","name":"Spring Boot","rationale":"Strong typing","tradeoffs":"JVM memory"}],
                  "components": [{"id":"COMP-001","name":"CatalogService","responsibility":"Product catalog"}],
                  "databaseEntities": [{"id":"ENT-001","name":"Product","fields":["id","sku","price"]}],
                  "apiContracts": [{"id":"API-001","method":"GET","path":"/api/v1/products","summary":"List products"}],
                  "decisions": [{"id":"ADR-001","title":"Modular Monolith","decision":"Start modular","rationale":"Faster iteration"}]
                }
                """;

        LlmProvider stubLlm = new LlmProvider() {
            int calls = 0;
            @Override public String providerName() { return "test-gemini"; }
            @Override public String activeModel() { return "gemini-2.5-flash"; }
            @Override public boolean isConfigured() { return true; }
            @Override public LlmResponse generateJson(LlmRequest request) {
                calls++;
                if (calls == 1) return new LlmResponse("gemini-2.5-flash", validDiscoveryJson, 100, 120, 0.0001, 50);
                if (calls == 2) return new LlmResponse("gemini-2.5-flash", invalidArchitectJson, 100, 40, 0.0001, 40);
                return new LlmResponse("gemini-2.5-flash", validArchitectJson, 120, 180, 0.0002, 60);
            }
        };

        WorkflowOrchestratorService orchestrator = new WorkflowOrchestratorService(
                workflowRepository, usageRepository, documentRepository,
                new ProjectAuthorizationService(projectRepository),
                stubLlm,
                List.of(
                        new SpecializedAgents.DiscoveryAgent(),
                        new SpecializedAgents.ArchitectAgent(),
                        new SpecializedAgents.EngineeringAgent(),
                        new SpecializedAgents.QualityDeliveryAgent()
                ),
                auditService, testProps(), objectMapper, fixedClock
        );

        WorkflowExecution paused = orchestrator.startWorkflow(
                "proj-ecom", userA, List.of(AgentType.DISCOVERY, AgentType.ARCHITECT), true, ""
        );
        assertThat(paused.getStatus()).isEqualTo("AWAITING_APPROVAL");
        assertThat(paused.getSteps().get(0).getStatus()).isEqualTo("AWAITING_APPROVAL");

        when(workflowRepository.findById("wf-1")).thenReturn(Optional.of(paused));
        WorkflowExecution failed = orchestrator.approveCheckpoint("proj-ecom", "wf-1", userA);
        assertThat(failed.getStatus()).isEqualTo("FAILED");
        assertThat(failed.getErrorMessage()).contains("missing required field");

        WorkflowExecution recovered = orchestrator.retryFailed("proj-ecom", "wf-1", userA);
        assertThat(recovered.getStatus()).isEqualTo("COMPLETED");
        assertThat(recovered.getSteps().get(1).getRetryCount()).isEqualTo(1);
    }

    @Test
    void scenarioC_academicRubricInconsistencyAndConstraintOverrideDetection() {
        Project project = new Project();
        project.setId("proj-acad");
        project.setOwnerId("user-a");
        when(projectRepository.findById("proj-acad")).thenReturn(Optional.of(project));
        when(academicRepository.findByProjectId("proj-acad")).thenReturn(Optional.empty());
        when(academicRepository.save(any(AcademicAssessment.class))).thenAnswer(inv -> inv.getArgument(0));

        AcademicService academicService = new AcademicService(academicRepository, new ProjectAuthorizationService(projectRepository));
        AcademicAssessment input = new AcademicAssessment();
        input.setGroupSize(3);
        input.setDeclaredTotalMarks(100.0);
        input.setMandatoryTechnologies(List.of("PostgreSQL", "React"));
        input.setForbiddenTechnologies(List.of("Firebase", "React"));
        input.setRubricCriteria(List.of(
                new AcademicAssessment.RubricCriterion("RUB-001", "Architecture", 30, "Distinction", "brief p.2", List.of("REQ-001"), "PLANNED", "NOT_VERIFIED"),
                new AcademicAssessment.RubricCriterion("RUB-002", "Implementation", 40, "Distinction", "brief p.3", List.of("REQ-002"), "PLANNED", "NOT_VERIFIED"),
                new AcademicAssessment.RubricCriterion("RUB-003", "Testing", 20, "Distinction", "brief p.4", List.of(), "UNMAPPED", "NOT_VERIFIED")
        ));

        AcademicAssessment saved = academicService.saveAndAudit("proj-acad", userA, input);
        assertThat(saved.getDetectedInconsistencies()).anyMatch(s -> s.contains("sum to 90.00"));
        assertThat(saved.getDetectedInconsistencies()).anyMatch(s -> s.contains("both mandatory and forbidden"));

        TechnologyCatalogService techService = new TechnologyCatalogService(techCatalogRepository);
        var validation = techService.validateSelection(
                List.of("Firebase", "CustomGraphDB"),
                List.of("Must use PostgreSQL"),
                List.of("Firebase")
        );
        assertThat(validation.valid()).isFalse();
        assertThat(validation.violations()).hasSize(2);
    }

    @Test
    void scenarioE_mlNotebookOmitsApiDbAndProfilesDatasetDeterministically() {
        Project mlProject = new Project();
        mlProject.setId("proj-ml");
        mlProject.setOwnerId("user-a");
        mlProject.setMode(ProjectMode.INDIVIDUAL);
        mlProject.setArchetypes(List.of("Machine Learning", "Data Science"));

        when(projectRepository.findById("proj-ml")).thenReturn(Optional.of(mlProject));
        when(specRepository.findFirstByProjectIdOrderByVersionNumberDesc("proj-ml")).thenReturn(Optional.empty());
        when(specRepository.save(any(ProjectSpec.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectSpecService specService = new ProjectSpecService(
                specRepository, projectRepository, new ProjectAuthorizationService(projectRepository), auditService, fixedClock
        );
        ProjectSpec arch = specService.generateArchitectureScaffold("proj-ml", userA);
        assertThat(arch.getArchitectureStyle()).isEqualTo("ML Pipeline Architecture");
        assertThat(arch.getApiContracts()).isEmpty();
        assertThat(arch.getDatabaseEntities()).isEmpty();

        String csv = """
                customer_id,tenure_months,monthly_spend,future_churn_leak,churned
                C001,12,49.50,0,0
                C002,3,19.00,1,1
                C002,3,19.00,1,1
                C003,24,,0,0
                """;
        when(datasetRepository.save(any(DatasetProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        DatasetIntelligenceService dsService = new DatasetIntelligenceService(datasetRepository, new ProjectAuthorizationService(projectRepository));

        DatasetProfile profile = dsService.profileCsv("proj-ml", userA, "churn.csv", csv.getBytes(StandardCharsets.UTF_8), "churned", "CLASSIFICATION");
        assertThat(profile.getRowCount()).isEqualTo(4);
        assertThat(profile.getDuplicateRowCount()).isEqualTo(1);
        assertThat(profile.getSha256Fingerprint()).hasSize(64);
        assertThat(profile.getLeakageRiskWarnings()).anyMatch(w -> w.contains("future_churn_leak"));
        assertThat(profile.getExperimentPlan().baselineModels()).contains("LogisticRegression");
    }

    @Test
    void projectAwareChatProposesSpecChangeAndRequiresExplicitApproval() {
        Project project = new Project();
        project.setId("proj-chat");
        project.setOwnerId("user-a");
        project.setName("Chat Test Project");
        project.setMode(ProjectMode.CLIENT);

        ChatConversation conv = new ChatConversation();
        conv.setId("conv-1");
        conv.setProjectId("proj-chat");
        conv.setUserId("user-a");
        conv.setTitle("Architecture Q&A");

        when(projectRepository.findById("proj-chat")).thenReturn(Optional.of(project));
        when(chatRepository.findById("conv-1")).thenReturn(Optional.of(conv));
        when(chatRepository.save(any(ChatConversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(specRepository.findFirstByProjectIdOrderByVersionNumberDesc("proj-chat")).thenReturn(Optional.empty());
        when(specRepository.save(any(ProjectSpec.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.findByProjectIdOrderByCreatedAtDesc("proj-chat")).thenReturn(List.of());

        LlmProvider chatLlm = new LlmProvider() {
            @Override public String providerName() { return "test-gemini"; }
            @Override public String activeModel() { return "gemini-2.5-flash"; }
            @Override public boolean isConfigured() { return true; }
            @Override public LlmResponse generateJson(LlmRequest request) {
                return new LlmResponse("gemini-2.5-flash", """
                        {
                          "answer": "You should add rate limiting to the public API.",
                          "sourceRefs": ["Project Context"],
                          "suggestedActions": ["Add NFR for rate limiting"],
                          "proposedSpecChange": {
                            "targetSection": "functionalRequirements",
                            "summary": "Add API rate limiting requirement",
                            "title": "API Rate Limiting",
                            "description": "Limit public endpoints to 100 req/min per IP."
                          }
                        }
                        """, 80, 90, 0.0001, 45);
            }
        };

        ProjectAuthorizationService auth = new ProjectAuthorizationService(projectRepository);
        ProjectSpecService specService = new ProjectSpecService(specRepository, projectRepository, auth, auditService, fixedClock);
        ProjectChatService chatService = new ProjectChatService(
                chatRepository, auth, specRepository, specService, documentRepository, chatLlm, usageRepository, objectMapper, fixedClock
        );

        ChatConversation updated = chatService.sendMessage("proj-chat", "conv-1", userA, "What requirements are missing?");
        assertThat(updated.getMessages()).hasSize(2);
        String proposalId = updated.getMessages().get(1).specProposal().proposalId();
        assertThat(updated.getMessages().get(1).specProposal().status()).isEqualTo("PENDING_APPROVAL");

        ProjectSpec specAfterApproval = chatService.approveSpecProposal("proj-chat", "conv-1", proposalId, userA);
        assertThat(specAfterApproval.getFunctionalRequirements()).anyMatch(r -> r.title().equals("API Rate Limiting"));
    }
}
