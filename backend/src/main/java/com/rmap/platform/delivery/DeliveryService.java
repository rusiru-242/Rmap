package com.rmap.platform.delivery;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.execution.ExecutionService;
import com.rmap.platform.project.Project;
import com.rmap.platform.project.ProjectAuthorizationService;
import com.rmap.platform.spec.AcademicAssessmentRepository;
import com.rmap.platform.spec.ProjectSpec;
import com.rmap.platform.spec.ProjectSpecRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DeliveryService {
    private final DesignArtifactRepository designRepository;
    private final GeneratedDocumentRepository docRepository;
    private final MlExperimentRepository mlRepository;
    private final DeploymentPlanRepository deploymentRepository;
    private final ProjectSpecRepository specRepository;
    private final AcademicAssessmentRepository academicRepository;
    private final ProjectAuthorizationService projectAuth;
    private final ExecutionService executionService;

    public DeliveryService(
            DesignArtifactRepository designRepository,
            GeneratedDocumentRepository docRepository,
            MlExperimentRepository mlRepository,
            DeploymentPlanRepository deploymentRepository,
            ProjectSpecRepository specRepository,
            AcademicAssessmentRepository academicRepository,
            ProjectAuthorizationService projectAuth,
            ExecutionService executionService
    ) {
        this.designRepository = designRepository;
        this.docRepository = docRepository;
        this.mlRepository = mlRepository;
        this.deploymentRepository = deploymentRepository;
        this.specRepository = specRepository;
        this.academicRepository = academicRepository;
        this.projectAuth = projectAuth;
        this.executionService = executionService;
    }

    // =========================================================================
    // 1. UI/UX DESIGN STUDIO & GOOGLE STITCH PROMPTS
    // =========================================================================

    public DesignArtifact generateStitchPrompt(String projectId, AuthenticatedUser user, String targetSurface, String visualNotes) {
        Project project = projectAuth.requireEditor(projectId, user);
        ProjectSpec spec = specRepository.findFirstByProjectIdOrderByVersionNumberDesc(projectId).orElse(null);

        String surface = (targetSurface == null || targetSurface.isBlank()) ? "WEB" : targetSurface.toUpperCase(Locale.ROOT);
        List<String> pages = spec == null || spec.getUiPages().isEmpty()
                ? List.of("Dashboard", "Core Workspace", "Detail View", "Settings")
                : spec.getUiPages().stream().map(ProjectSpec.UiScreenItem::name).toList();

        String prompt = """
                Design a clean, production-ready %s interface for "%s".
                Mode: %s | Archetypes: %s
                Visual Direction: Minimal Linear / VS Code developer aesthetic, neutral palette, single restrained accent, compact typography, light & dark modes, accessible keyboard focus states, loading/empty/error/validation states.
                Pages/Screens: %s
                Additional Visual Notes: %s
                Strict Preservation Rules: Preserve all existing API integrations, authentication flows, state management, and routing. Do NOT replace working functionality with static mockups.
                """.formatted(surface, project.getName(), project.getMode(), project.getArchetypes(), pages, visualNotes == null ? "None" : visualNotes);

        DesignArtifact artifact = new DesignArtifact();
        artifact.setProjectId(projectId);
        artifact.setArtifactType("STITCH_PROMPT");
        artifact.setTargetSurface(surface);
        artifact.setTitle("Google Stitch Design Prompt (" + surface + ")");
        artifact.setStitchPrompt(prompt);
        artifact.setPagesInventory(pages);
        artifact.setComponentMappings(pages.stream().map(p -> p + " -> Map to existing route & real API controller").toList());
        artifact.setPreservationRules(List.of(
                "Preserve existing authentication & JWT interceptors",
                "Preserve existing API contracts and validation rules",
                "Maintain responsive breakpoints and loading/empty/error states"
        ));
        return designRepository.save(artifact);
    }

    public List<DesignArtifact> listDesignArtifacts(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return designRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    // =========================================================================
    // 2. DOCUMENTATION ENGINE (MARKDOWN, PDF, DOCX EXPORTS)
    // =========================================================================

    public GeneratedDocument generateDocumentation(String projectId, AuthenticatedUser user, String docType) {
        Project project = projectAuth.requireEditor(projectId, user);
        ProjectSpec spec = specRepository.findFirstByProjectIdOrderByVersionNumberDesc(projectId).orElse(null);
        Map<String, Object> trace = executionService.getTraceabilityMatrix(projectId, user);

        String type = (docType == null || docType.isBlank()) ? "SRS" : docType.trim().toUpperCase(Locale.ROOT);
        int specVer = spec == null ? 0 : spec.getVersionNumber();

        StringBuilder md = new StringBuilder();
        md.append("# ").append(project.getName()).append(" — ").append(type).append("\n\n");
        md.append("- **Source ProjectSpec Version**: v").append(specVer).append("\n");
        md.append("- **Project Mode**: ").append(project.getMode()).append("\n");
        md.append("- **Archetypes**: ").append(project.getArchetypes()).append("\n\n");
        md.append("## Status Separation (Integrity Policy)\n");
        md.append("- **Planned Coverage**: ").append(trace.get("planningCoveragePercent")).append("%\n");
        md.append("- **Implementation Coverage**: ").append(trace.get("implementationCoveragePercent")).append("%\n");
        md.append("- **Verified Evidence Coverage**: ").append(trace.get("verifiedCoveragePercent")).append("%\n\n");
        md.append("## Overview & Problem Statement\n").append(project.getDescription()).append("\n\n");
        md.append("## Functional Requirements\n");
        if (spec != null && !spec.getFunctionalRequirements().isEmpty()) {
            for (var r : spec.getFunctionalRequirements()) {
                md.append("- **").append(r.id()).append("** (").append(r.priority()).append("): ").append(r.title())
                        .append(" — ").append(r.description()).append(" _[Source: ").append(r.sourceRef()).append("]_\n");
            }
        } else {
            md.append("- No functional requirements recorded yet.\n");
        }
        md.append("\n## Architecture & Constraints\n- Architecture Style: ")
                .append(spec == null ? "Not generated" : spec.getArchitectureStyle())
                .append("\n- Mandatory Constraints: ").append(project.getMandatoryConstraints()).append("\n");

        GeneratedDocument doc = new GeneratedDocument();
        doc.setProjectId(projectId);
        doc.setSpecVersion(specVer);
        doc.setDocType(type);
        doc.setTitle(project.getName() + " " + type);
        doc.setVersionNumber((int) docRepository.countByProjectIdAndDocType(projectId, type) + 1);
        doc.setMarkdownContent(md.toString());
        doc.setCreatedBy(user.email());
        return docRepository.save(doc);
    }

    public List<GeneratedDocument> listDocuments(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return docRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public byte[] exportDocumentBytes(String projectId, String docId, AuthenticatedUser user, String format) {
        projectAuth.requireMember(projectId, user);
        GeneratedDocument doc = docRepository.findById(docId)
                .filter(d -> d.getProjectId().equals(projectId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "GENERATED_DOC_NOT_FOUND", "Generated document not found."));

        String fmt = (format == null ? "MD" : format.trim().toUpperCase(Locale.ROOT));
        return switch (fmt) {
            case "PDF" -> renderPdf(doc.getTitle(), doc.getMarkdownContent());
            case "DOCX" -> renderDocx(doc.getTitle(), doc.getMarkdownContent());
            default -> doc.getMarkdownContent().getBytes(StandardCharsets.UTF_8);
        };
    }

    private byte[] renderPdf(String title, String content) {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            pdf.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(pdf, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 13);
                cs.newLineAtOffset(50, 750);
                cs.showText(sanitizePdfText(title));
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cs.setLeading(14f);
                cs.newLine();
                cs.newLine();
                int lineCount = 0;
                for (String rawLine : content.split("\\r?\\n")) {
                    if (lineCount++ > 45) break;
                    String safe = sanitizePdfText(rawLine);
                    cs.showText(safe.length() > 95 ? safe.substring(0, 95) : safe);
                    cs.newLine();
                }
                cs.endText();
            }
            pdf.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF_EXPORT_FAILED", "Failed to render PDF: " + ex.getMessage());
        }
    }

    private byte[] renderDocx(String title, String content) {
        try (XWPFDocument docx = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFParagraph heading = docx.createParagraph();
            var hRun = heading.createRun();
            hRun.setBold(true);
            hRun.setFontSize(14);
            hRun.setText(title);

            for (String line : content.split("\\r?\\n")) {
                XWPFParagraph p = docx.createParagraph();
                p.createRun().setText(line);
            }
            docx.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "DOCX_EXPORT_FAILED", "Failed to render DOCX: " + ex.getMessage());
        }
    }

    private String sanitizePdfText(String s) {
        return s.replaceAll("[^\\x20-\\x7E]", " ");
    }

    // =========================================================================
    // 3. MACHINE LEARNING EXPERIMENT STUDIO (REAL IMPORTED RESULTS ONLY)
    // =========================================================================

    public MlExperiment createExperiment(String projectId, AuthenticatedUser user, MlExperiment req) {
        projectAuth.requireEditor(projectId, user);
        req.setId(null);
        req.setProjectId(projectId);
        req.setStatus("PLANNED");
        req.setImportedMetrics(new LinkedHashMap<>()); // Never fabricate metrics at creation time
        return mlRepository.save(req);
    }

    public MlExperiment importRealExperimentResults(String projectId, String expId, AuthenticatedUser user, String resultSource, Map<String, Double> metrics, String notes) {
        projectAuth.requireEditor(projectId, user);
        if (resultSource == null || resultSource.isBlank() || metrics == null || metrics.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REAL_RESULT_SOURCE_REQUIRED",
                    "Importing ML metrics requires a real script/notebook resultSource and observed numeric metrics.");
        }
        MlExperiment exp = mlRepository.findById(expId)
                .filter(e -> e.getProjectId().equals(projectId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EXPERIMENT_NOT_FOUND", "ML experiment not found."));
        exp.setResultSource(resultSource.trim());
        exp.setImportedMetrics(metrics);
        exp.setStatus("RESULTS_IMPORTED");
        if (notes != null) exp.setNotes(notes);
        return mlRepository.save(exp);
    }

    public List<MlExperiment> listExperiments(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return mlRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    // =========================================================================
    // 4. DEPLOYMENT CENTER (DATA-DRIVEN CATALOG + 20-STEP MANUAL GUIDE)
    // =========================================================================

    public List<Map<String, Object>> hostingCatalog() {
        return List.of(
                Map.of("provider", "Vercel", "bestFor", List.of("React", "Next.js", "Static"), "verifiedDate", "2026-11-27", "pricingNote", "Verify current provider pricing & limits on provider site"),
                Map.of("provider", "Cloudflare Pages", "bestFor", List.of("Static", "React", "Vue", "Svelte"), "verifiedDate", "2026-11-27", "pricingNote", "Verify current provider pricing & limits on provider site"),
                Map.of("provider", "Netlify", "bestFor", List.of("Static", "React", "Frontend"), "verifiedDate", "2026-11-27", "pricingNote", "Verify current provider pricing & limits on provider site"),
                Map.of("provider", "Render", "bestFor", List.of("Spring Boot", "Node.js", "Python", "Docker"), "verifiedDate", "2026-11-27", "pricingNote", "Verify current provider pricing & limits on provider site"),
                Map.of("provider", "Railway", "bestFor", List.of("Full-Stack", "Docker", "PostgreSQL", "Redis"), "verifiedDate", "2026-11-27", "pricingNote", "Verify current provider pricing & limits on provider site"),
                Map.of("provider", "MongoDB Atlas", "bestFor", List.of("MongoDB Replica Set"), "verifiedDate", "2026-11-27", "pricingNote", "Verify current provider pricing & limits on provider site"),
                Map.of("provider", "Supabase", "bestFor", List.of("PostgreSQL", "Auth", "Storage"), "verifiedDate", "2026-11-27", "pricingNote", "Verify current provider pricing & limits on provider site"),
                Map.of("provider", "AWS / Azure / Google Cloud", "bestFor", List.of("Enterprise", "Cloud Run", "AKS", "ECS"), "verifiedDate", "2026-11-27", "pricingNote", "Verify current provider pricing & limits on provider site")
        );
    }

    public DeploymentPlan generateDeploymentPlan(String projectId, AuthenticatedUser user, String provider, String targetType, String rootDir, String buildCmd, String startCmd) {
        Project project = projectAuth.requireEditor(projectId, user);
        String prov = (provider == null || provider.isBlank()) ? "Render" : provider.trim();
        String root = (rootDir == null || rootDir.isBlank()) ? "." : rootDir.trim();
        String build = (buildCmd == null || buildCmd.isBlank()) ? "npm run build" : buildCmd.trim();
        String start = (startCmd == null || startCmd.isBlank()) ? "npm start" : startCmd.trim();

        List<String> steps = List.of(
                "1. Open the official " + prov + " website in your browser.",
                "2. Sign in or create a new " + prov + " account.",
                "3. Open the main " + prov + " Dashboard.",
                "4. Click 'New' / 'Add Project' / 'Create Web Service'.",
                "5. Connect your GitHub account with least-privilege repository access.",
                "6. Select the GitHub repository for " + project.getName() + ".",
                "7. Choose the target release branch (e.g., main).",
                "8. Set Root Directory to: " + root,
                "9. Select the matching runtime / framework (" + (targetType == null ? "Auto-detected" : targetType) + ").",
                "10. Set Build Command to: " + build,
                "11. Set Start Command to: " + start,
                "12. Open the Environment Variables section and add all required secrets (never commit .env files).",
                "13. Attach or configure your external database connection URI (e.g., MongoDB Atlas / PostgreSQL).",
                "14. Review billing/free-tier limits and HTTPS/CORS security settings before deploying.",
                "15. Click 'Deploy' to trigger the initial build.",
                "16. Watch the live build & startup logs for compilation or missing-env errors.",
                "17. Open the generated live deployment URL once the deploy reports healthy.",
                "18. Verify /health and critical authenticated workflows on the live deployment.",
                "19. Record the verified live URL back in Rmap Deployment Center.",
                "20. If deployment fails, copy the exact log lines into Rmap Error Fix Studio for targeted diagnosis."
        );

        DeploymentPlan plan = new DeploymentPlan();
        plan.setProjectId(projectId);
        plan.setProvider(prov);
        plan.setTargetType(targetType == null ? "WEB_SERVICE" : targetType);
        plan.setBranchName("main");
        plan.setRootDirectory(root);
        plan.setBuildCommand(build);
        plan.setStartCommand(start);
        plan.setRequiredEnvVars(List.of("PORT", "DATABASE_URL", "JWT_SECRET"));
        plan.setClickByClickManualSteps(steps);
        plan.setProviderGuidanceNotice("Provider dashboard button labels may change over time; verify current pricing and UI directly on " + prov + ".");
        plan.setVerificationStatus("PLANNED");
        return deploymentRepository.save(plan);
    }

    public List<DeploymentPlan> listDeploymentPlans(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return deploymentRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    // =========================================================================
    // 5. ACADEMIC DELIVERY & VIVA PREP + TESTING & SECURITY + CLIENT CHANGE IMPACT
    // =========================================================================

    public Map<String, Object> getAcademicDeliveryPack(String projectId, AuthenticatedUser user) {
        Project project = projectAuth.requireMember(projectId, user);
        var acad = academicRepository.findByProjectId(projectId).orElse(null);
        var trace = executionService.getTraceabilityMatrix(projectId, user);
        var aiLogs = executionService.listAiUsageLogs(projectId, user);

        List<String> vivaQuestions = List.of(
                "Walk through the architecture of " + project.getName() + " and explain why this pattern was chosen over alternatives.",
                "How are authentication, authorization, and secret management enforced in your actual codebase?",
                "Which mandatory constraints (" + project.getMandatoryConstraints() + ") shaped your database and API design?",
                "Demonstrate a live test failure or edge case and explain how your error handling recovers safely."
        );

        Map<String, Object> pack = new LinkedHashMap<>();
        pack.put("projectName", project.getName());
        pack.put("traceabilitySummary", trace);
        pack.put("aiUsageLogCount", aiLogs.size());
        pack.put("vivaPreparationQuestions", vivaQuestions);
        pack.put("submissionChecklist", List.of(
                "Verify archive/folder naming matches the assignment brief",
                "Exclude .env, node_modules, target, build, and private keys",
                "Include transparent AI Usage Log (ACAD-14)",
                "Verify all student-authored reflections are written in your own words (no ghostwriting)"
        ));
        pack.put("inconsistencies", acad == null ? List.of() : acad.getDetectedInconsistencies());
        return pack;
    }

    public Map<String, Object> getTestingAndSecurityGuidance(String projectId, AuthenticatedUser user) {
        Project project = projectAuth.requireMember(projectId, user);
        return Map.of(
                "projectName", project.getName(),
                "recommendedTestLayers", List.of("Unit Tests", "Integration / API Tests", "Security & RBAC Tests", "Regression Tests"),
                "securityChecklist", List.of(
                        "Verify JWT & RBAC authorization on every protected endpoint",
                        "Confirm zero hardcoded secrets or committed .env files",
                        "Validate all DTOs and file upload bounds (path traversal & archive bomb checks)",
                        "Never treat uploaded documents or repository text as executable AI instructions"
                ),
                "disclaimer", "Passing automated checks does not by itself prove complete security; perform manual threat review before production release."
        );
    }

    public Map<String, Object> processClientChangeRequest(String projectId, AuthenticatedUser user, String requirementId, String changeSummary) {
        projectAuth.requireEditor(projectId, user);
        int invalidated = executionService.invalidateEvidenceForRequirement(projectId, user, requirementId);
        return Map.of(
                "projectId", projectId,
                "requirementId", requirementId,
                "changeSummary", changeSummary,
                "staleEvidenceInvalidated", invalidated,
                "nextSteps", List.of(
                        "Update ProjectSpec draft for " + requirementId,
                        "Re-run impacted unit and regression tests",
                        "Record fresh verification evidence to restore VERIFIED status"
                )
        );
    }
}
