package com.rmap.platform.execution;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.AuditService;
import com.rmap.platform.project.Project;
import com.rmap.platform.project.ProjectAuthorizationService;
import com.rmap.platform.project.ProjectMode;
import com.rmap.platform.spec.AcademicAssessment;
import com.rmap.platform.spec.AcademicAssessmentRepository;
import com.rmap.platform.spec.ProjectSpec;
import com.rmap.platform.spec.ProjectSpecRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ExecutionService {
    private static final Pattern SECRET_REDACT = Pattern.compile(
            "(?i)(api[_-]?key|secret|password|bearer|private[_-]?key|token)\\s*[:=]\\s*\\S+"
    );

    private final PhasePlanRepository phasePlanRepository;
    private final ProjectTaskRepository taskRepository;
    private final MasterPromptVersionRepository promptRepository;
    private final CodingAgentExecutionRepository agentExecutionRepository;
    private final VerificationEvidenceRepository evidenceRepository;
    private final ErrorIncidentRepository errorRepository;
    private final AiUsageLogEntryRepository aiUsageLogRepository;
    private final ProjectSpecRepository specRepository;
    private final AcademicAssessmentRepository academicRepository;
    private final ProjectAuthorizationService projectAuth;
    private final AuditService auditService;
    private final Clock clock;

    public ExecutionService(
            PhasePlanRepository phasePlanRepository,
            ProjectTaskRepository taskRepository,
            MasterPromptVersionRepository promptRepository,
            CodingAgentExecutionRepository agentExecutionRepository,
            VerificationEvidenceRepository evidenceRepository,
            ErrorIncidentRepository errorRepository,
            AiUsageLogEntryRepository aiUsageLogRepository,
            ProjectSpecRepository specRepository,
            AcademicAssessmentRepository academicRepository,
            ProjectAuthorizationService projectAuth,
            AuditService auditService,
            Clock clock
    ) {
        this.phasePlanRepository = phasePlanRepository;
        this.taskRepository = taskRepository;
        this.promptRepository = promptRepository;
        this.agentExecutionRepository = agentExecutionRepository;
        this.evidenceRepository = evidenceRepository;
        this.errorRepository = errorRepository;
        this.aiUsageLogRepository = aiUsageLogRepository;
        this.specRepository = specRepository;
        this.academicRepository = academicRepository;
        this.projectAuth = projectAuth;
        this.auditService = auditService;
        this.clock = clock;
    }

    // =========================================================================
    // 1. INTELLIGENT PHASE OPTIMIZATION ENGINE (QUICK / STANDARD / CUSTOM)
    // =========================================================================

    public PhasePlan generateOrReplanPhases(String projectId, AuthenticatedUser user, String mode, Integer customPhaseCount) {
        Project project = projectAuth.requireEditor(projectId, user);
        ProjectSpec spec = specRepository.findFirstByProjectIdOrderByVersionNumberDesc(projectId).orElse(null);
        String resolvedMode = (mode == null || mode.isBlank()) ? "STANDARD" : mode.trim().toUpperCase(Locale.ROOT);

        PhasePlan existing = phasePlanRepository.findByProjectId(projectId).orElse(null);
        List<PhasePlan.PhaseItem> preservedVerified = new ArrayList<>();
        if (existing != null) {
            for (var ph : existing.getPhases()) {
                if ("VERIFIED".equals(ph.status())) {
                    preservedVerified.add(ph);
                }
            }
        }

        List<String> reqIds = spec == null ? List.of()
                : spec.getFunctionalRequirements().stream().map(ProjectSpec.RequirementItem::id).toList();
        List<String> members = project.getMembers().stream().map(Project.ProjectMember::email).toList();

        boolean isMlOnly = isStandaloneMl(project);
        List<PhasePlan.PhaseItem> generated = new ArrayList<>(preservedVerified);
        int startIdx = preservedVerified.size() + 1;

        int targetCount = switch (resolvedMode) {
            case "QUICK" -> isMlOnly ? 2 : 3; // 1–4 phases
            case "CUSTOM" -> Math.max(1, Math.min(12, customPhaseCount == null ? 4 : customPhaseCount));
            default -> isMlOnly ? 5 : 6; // 5–10 phases (STANDARD)
        };

        List<String> templates = isMlOnly
                ? List.of("Data Ingestion & Profiling", "EDA & Preprocessing Pipeline", "Baseline & Candidate Modeling", "Hyperparameter Tuning & Validation", "Reproducibility & Model Report", "Packaging & Handover")
                : ("QUICK".equals(resolvedMode)
                    ? List.of("Foundation, Core Schema & Auth", "Core Feature Implementation", "Verification, Hardening & Delivery", "Post-MVP Polish")
                    : List.of("Foundation & Environment Setup", "Data Layer & Core Domain", "API & Business Logic", "Frontend / Client Workspace", "Integration, Security & Testing", "Deployment & Client Handover", "Observability & Maintenance"));

        int needed = Math.max(1, targetCount - preservedVerified.size());
        for (int i = 0; i < needed; i++) {
            String phId = String.format("PH-%03d", startIdx + i);
            String prevId = (startIdx + i) > 1 ? String.format("PH-%03d", startIdx + i - 1) : null;
            String title = templates.get(Math.min(i, templates.size() - 1));
            List<String> assignedReqs = partitionSlice(reqIds, i, needed);

            generated.add(new PhasePlan.PhaseItem(
                    phId,
                    title,
                    "Deliver " + title + " aligned with " + project.getName() + " (" + resolvedMode + " mode).",
                    "Scoped to assigned requirements and verified artifacts.",
                    prevId == null ? List.of("Approved ProjectSpec") : List.of("Completed " + prevId),
                    prevId == null ? List.of() : List.of(prevId),
                    assignedReqs,
                    members,
                    List.of(title + " source code & automated tests"),
                    "Implement strictly according to approved ProjectSpec v" + (spec == null ? 0 : spec.getVersionNumber()) + ".",
                    List.of("All assigned requirements implemented", "Build & automated tests pass with real evidence"),
                    List.of("Run project build command", "Run project test suite"),
                    "QUICK".equals(resolvedMode) ? "1–4 hours (time-boxed)" : "1–3 days (±30%)",
                    "PLANNED",
                    List.of(),
                    List.of()
            ));
        }

        validateNoCircularDependencies(generated);

        PhasePlan plan = existing != null ? existing : new PhasePlan();
        plan.setProjectId(projectId);
        plan.setSpecVersion(spec == null ? 0 : spec.getVersionNumber());
        plan.setPlanningMode(resolvedMode);
        plan.setPhases(generated);
        plan = phasePlanRepository.save(plan);

        auditService.record(user.userId(), user.email(), "PHASE_PLAN_GENERATED", "PHASE_PLAN", plan.getId(), Map.of(
                "mode", resolvedMode,
                "phaseCount", generated.size(),
                "preservedVerified", preservedVerified.size()
        ));
        return plan;
    }

    public PhasePlan saveCustomPlan(String projectId, AuthenticatedUser user, PhasePlan incoming) {
        projectAuth.requireEditor(projectId, user);
        validateNoCircularDependencies(incoming.getPhases());
        PhasePlan plan = phasePlanRepository.findByProjectId(projectId).orElseGet(() -> {
            PhasePlan p = new PhasePlan();
            p.setProjectId(projectId);
            return p;
        });
        plan.setPlanningMode(incoming.getPlanningMode() == null ? "CUSTOM" : incoming.getPlanningMode());
        plan.setPhases(incoming.getPhases());
        return phasePlanRepository.save(plan);
    }

    public PhasePlan getPhasePlan(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return phasePlanRepository.findByProjectId(projectId).orElse(null);
    }

    public void validateNoCircularDependencies(List<PhasePlan.PhaseItem> phases) {
        if (phases == null) return;
        Map<String, List<String>> graph = new HashMap<>();
        for (var p : phases) {
            graph.put(p.phaseId(), p.dependencies() == null ? List.of() : p.dependencies());
        }
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (String id : graph.keySet()) {
            if (hasCycle(id, graph, visiting, visited)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "CIRCULAR_DEPENDENCY_DETECTED",
                        "Circular phase dependency detected involving phase " + id + ".");
            }
        }
    }

    private boolean hasCycle(String node, Map<String, List<String>> graph, Set<String> visiting, Set<String> visited) {
        if (visiting.contains(node)) return true;
        if (visited.contains(node)) return false;
        visiting.add(node);
        for (String dep : graph.getOrDefault(node, List.of())) {
            if (graph.containsKey(dep) && hasCycle(dep, graph, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(node);
        visited.add(node);
        return false;
    }

    // =========================================================================
    // 2. TASK MANAGEMENT (EVIDENCE-GATED VERIFICATION)
    // =========================================================================

    public ProjectTask createTask(String projectId, AuthenticatedUser user, ProjectTask req) {
        projectAuth.requireEditor(projectId, user);
        long nextNum = taskRepository.countByProjectId(projectId) + 1;
        req.setId(null);
        req.setProjectId(projectId);
        req.setTaskCode(String.format("TASK-%03d", nextNum));
        if (req.getStatus() == null || req.getStatus().isBlank() || "VERIFIED".equals(req.getStatus())) {
            req.setStatus("PLANNED");
        }
        if (req.getPriority() == null || req.getPriority().isBlank()) {
            req.setPriority("MEDIUM");
        }
        return taskRepository.save(req);
    }

    public List<ProjectTask> listTasks(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return taskRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
    }

    public ProjectTask updateTaskStatus(String projectId, String taskId, AuthenticatedUser user, String newStatus) {
        projectAuth.requireEditor(projectId, user);
        ProjectTask task = taskRepository.findById(taskId)
                .filter(t -> t.getProjectId().equals(projectId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found."));

        String normalized = newStatus.trim().toUpperCase(Locale.ROOT);
        if ("VERIFIED".equals(normalized)) {
            boolean hasValidEvidence = task.getEvidenceIds() != null && !task.getEvidenceIds().isEmpty();
            if (!hasValidEvidence && task.getRequirementIds() != null) {
                hasValidEvidence = task.getRequirementIds().stream()
                        .anyMatch(reqId -> evidenceRepository.findByProjectIdAndRequirementId(projectId, reqId).stream()
                                .anyMatch(e -> "VERIFIED".equals(e.getVerificationStatus())));
            }
            if (!hasValidEvidence) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VERIFICATION_EVIDENCE_REQUIRED",
                        "Cannot mark task VERIFIED without real build/test/manual verification evidence.");
            }
        }
        task.setStatus(normalized);
        return taskRepository.save(task);
    }

    // =========================================================================
    // 3. MASTER PROMPT GENERATION + ACAD-03 COVERAGE GATE + AI USAGE LOG
    // =========================================================================

    public MasterPromptVersion generateMasterPrompt(
            String projectId,
            AuthenticatedUser user,
            String phaseId,
            String targetTool,
            String promptType,
            String assignedMemberEmail,
            boolean enforceMandatoryGate
    ) {
        Project project = projectAuth.requireEditor(projectId, user);
        ProjectSpec spec = specRepository.findFirstByProjectIdOrderByVersionNumberDesc(projectId).orElse(null);
        PhasePlan plan = phasePlanRepository.findByProjectId(projectId).orElse(null);
        List<ProjectTask> tasks = taskRepository.findByProjectIdOrderByCreatedAtAsc(projectId);

        if (enforceMandatoryGate) {
            enforceMandatoryCoverageGate(project, spec, plan, tasks);
        }

        String tool = (targetTool == null || targetTool.isBlank()) ? "ANTIGRAVITY" : targetTool.trim().toUpperCase(Locale.ROOT);
        String type = (promptType == null || promptType.isBlank()) ? "PHASE_MASTER" : promptType.trim().toUpperCase(Locale.ROOT);

        PhasePlan.PhaseItem selectedPhase = plan == null ? null : plan.getPhases().stream()
                .filter(p -> phaseId != null && p.phaseId().equals(phaseId))
                .findFirst()
                .orElse(plan.getPhases().isEmpty() ? null : plan.getPhases().get(0));

        String promptText = build19SectionPrompt(project, spec, selectedPhase, tasks, tool, type, assignedMemberEmail);
        List<MasterPromptVersion.ContextFileEntry> contextFiles = buildToolContextFiles(project, spec, tool);

        MasterPromptVersion pv = new MasterPromptVersion();
        pv.setProjectId(projectId);
        pv.setSpecVersion(spec == null ? 0 : spec.getVersionNumber());
        pv.setPhaseId(selectedPhase == null ? (phaseId == null ? "PH-001" : phaseId) : selectedPhase.phaseId());
        pv.setTargetTool(tool);
        pv.setPromptType(type);
        pv.setAssignedMemberEmail(assignedMemberEmail);
        pv.setVersionNumber((int) promptRepository.countByProjectId(projectId) + 1);
        pv.setPromptContent(promptText);
        pv.setGeneratedContextFiles(contextFiles);
        pv.setCreatedBy(user.email());
        pv = promptRepository.save(pv);

        // Transparent AI Usage Log (ACAD-14)
        aiUsageLogRepository.save(new AiUsageLogEntry(
                projectId,
                user.email(),
                "PROMPT_GENERATED",
                tool,
                "Rmap MasterPromptEngine",
                type + " for " + pv.getPhaseId(),
                selectedPhase == null ? "" : String.join(",", selectedPhase.requirementIds()),
                redactSecrets(promptText.substring(0, Math.min(promptText.length(), 400))),
                "Pending human review and verification",
                clock.instant()
        ));

        return pv;
    }

    public List<MasterPromptVersion> listPrompts(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return promptRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public List<AiUsageLogEntry> listAiUsageLogs(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return aiUsageLogRepository.findByProjectIdOrderByTimestampDesc(projectId);
    }

    private void enforceMandatoryCoverageGate(Project project, ProjectSpec spec, PhasePlan plan, List<ProjectTask> tasks) {
        List<String> missing = new ArrayList<>();
        Set<String> coveredInPhases = new HashSet<>();
        if (plan != null) {
            for (var ph : plan.getPhases()) {
                if (ph.requirementIds() != null) coveredInPhases.addAll(ph.requirementIds());
            }
        }
        Set<String> coveredInTasks = new HashSet<>();
        for (var t : tasks) {
            if (t.getRequirementIds() != null) coveredInTasks.addAll(t.getRequirementIds());
        }

        if (spec != null) {
            for (var req : spec.getFunctionalRequirements()) {
                if ("HIGH".equalsIgnoreCase(req.priority()) || "CRITICAL".equalsIgnoreCase(req.priority())) {
                    if (!coveredInPhases.contains(req.id()) && !coveredInTasks.contains(req.id())) {
                        missing.add("Mandatory requirement " + req.id() + " (" + req.title() + ") is not assigned to any phase or task.");
                    }
                }
            }
        }

        if (project.getMode() == ProjectMode.ACADEMIC) {
            academicRepository.findByProjectId(project.getId()).ifPresent(acad -> {
                for (var rub : acad.getRubricCriteria()) {
                    if (rub.linkedRequirementIds() == null || rub.linkedRequirementIds().isEmpty()) {
                        missing.add("ACAD-03 violation: Rubric item " + rub.id() + " (" + rub.title() + ") has no mapped requirements.");
                    }
                }
                if (acad.getGroupSize() > 1 && acad.getMemberResponsibilities().size() < acad.getGroupSize()) {
                    missing.add("ACAD-03/05 violation: Team ownership incomplete for group size " + acad.getGroupSize() + ".");
                }
            });
        }

        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MANDATORY_COVERAGE_INCOMPLETE",
                    "Cannot generate final implementation prompt until mandatory coverage is complete: " + String.join(" | ", missing));
        }
    }

    private String build19SectionPrompt(
            Project project,
            ProjectSpec spec,
            PhasePlan.PhaseItem phase,
            List<ProjectTask> tasks,
            String tool,
            String type,
            String memberEmail
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Rmap Master Prompt (").append(type).append(" — Target: ").append(tool).append(")\n\n");
        sb.append("## 1. Agent Role\nPrincipal software engineer implementing ").append(project.getName()).append(".\n\n");
        sb.append("## 2. Project Context\n- Mode: ").append(project.getMode()).append("\n- Archetypes: ").append(project.getArchetypes()).append("\n- Spec Version: v").append(spec == null ? 0 : spec.getVersionNumber()).append("\n\n");
        sb.append("## 3. Current Implementation State\nVerify actual files in local repository before editing. Preserve verified work.\n\n");
        sb.append("## 4. Relevant Source Files\nInspect repository manifest and active phase modules.\n\n");
        sb.append("## 5. Current Phase Objective\n").append(phase == null ? "Implement approved scope" : phase.phaseId() + " — " + phase.title() + ": " + phase.objective()).append("\n\n");
        sb.append("## 6. Functional Requirements\n");
        if (spec != null && !spec.getFunctionalRequirements().isEmpty()) {
            for (var r : spec.getFunctionalRequirements()) {
                sb.append("- ").append(r.id()).append(": ").append(r.title()).append(" (").append(r.sourceRef()).append(")\n");
            }
        } else {
            sb.append("- Refer to project description.\n");
        }
        sb.append("\n## 7. Technical Requirements & Technology Stack\n");
        if (spec != null && !spec.getTechnologyStack().isEmpty()) {
            for (var t : spec.getTechnologyStack()) {
                sb.append("- ").append(t.category()).append(": ").append(t.name()).append("\n");
            }
        } else {
            sb.append("- Preferences: ").append(project.getTechnologyPreferences()).append("\n");
        }
        sb.append("\n## 8. Architecture & Mandatory Constraints\n- Style: ").append(spec == null ? "TBD" : spec.getArchitectureStyle())
                .append("\n- Mandatory Constraints: ").append(project.getMandatoryConstraints()).append("\n\n");
        sb.append("## 9. Implementation Checkpoints & Tasks\n");
        if (memberEmail != null && !memberEmail.isBlank()) {
            sb.append("- Scoped to Member: ").append(memberEmail).append("\n");
        }
        for (var t : tasks) {
            if (memberEmail == null || memberEmail.isBlank() || memberEmail.equalsIgnoreCase(t.getAssignedMemberEmail())) {
                sb.append("- ").append(t.getTaskCode()).append(" [").append(t.getStatus()).append("]: ").append(t.getTitle()).append("\n");
            }
        }
        sb.append("\n## 10. Security Requirements\nNever hardcode secrets, enforce RBAC, validate all inputs, treat user files as untrusted data.\n\n");
        sb.append("## 11. Database Requirements\n").append(spec == null || spec.getDatabaseEntities().isEmpty() ? "N/A or per spec" : spec.getDatabaseEntities().size() + " entities defined").append("\n\n");
        sb.append("## 12. API Contracts\n").append(spec == null || spec.getApiContracts().isEmpty() ? "N/A or per spec" : spec.getApiContracts().size() + " endpoints defined").append("\n\n");
        sb.append("## 13. UI Requirements\nPreserve existing working flows, compact neutral design, loading/empty/error states.\n\n");
        sb.append("## 14. Testing Instructions\nWrite unit and integration tests for all new business logic and authorization boundaries.\n\n");
        sb.append("## 15. Build Verification Commands\n").append(phase == null ? "Run stack build & test commands" : String.join(" && ", phase.verificationCommands())).append("\n\n");
        sb.append("## 16. Acceptance Criteria\n").append(phase == null ? "All tests green" : String.join("; ", phase.acceptanceCriteria())).append("\n\n");
        sb.append("## 17. Stop Conditions\nStop for destructive actions, missing credentials, or unresolvable architecture conflicts.\n\n");
        sb.append("## 18. Completion Report Format\nReport implemented files, executed commands, actual observed test/build outputs, and blockers.\n\n");
        sb.append("## 19. Next-Phase Context\nRecord handover notes for downstream phases without starting the next phase prematurely.\n");
        return sb.toString();
    }

    private List<MasterPromptVersion.ContextFileEntry> buildToolContextFiles(Project project, ProjectSpec spec, String tool) {
        List<MasterPromptVersion.ContextFileEntry> files = new ArrayList<>();
        String agentsMd = "# AGENTS.md — " + project.getName() + "\n\n"
                + "- Mode: " + project.getMode() + "\n"
                + "- Approved Spec Version: v" + (spec == null ? 0 : spec.getVersionNumber()) + "\n"
                + "- Mandatory Constraints: " + project.getMandatoryConstraints() + "\n"
                + "- Security: Never hardcode secrets, never fake tests or AI outputs, enforce authorization.\n";
        files.add(new MasterPromptVersion.ContextFileEntry("AGENTS.md", agentsMd));
        if ("CLAUDE_CODE".equals(tool) || "ANTIGRAVITY".equals(tool)) {
            files.add(new MasterPromptVersion.ContextFileEntry("CLAUDE.md", "# CLAUDE.md — " + project.getName() + "\n\nRead and follow AGENTS.md as authoritative instructions.\n"));
        }
        files.add(new MasterPromptVersion.ContextFileEntry("PHASE_REPORT_TEMPLATE.md", "# Phase Completion Report\n\n- Implemented Files:\n- Commands Executed:\n- Actual Test/Build Results:\n- Blockers:\n"));
        return files;
    }

    // =========================================================================
    // 4. EXTERNAL CODING AGENT ORCHESTRATION & ADAPTER REGISTRY
    // =========================================================================

    public List<Map<String, Object>> listCodingAgentAdapters() {
        return List.of(
                Map.of("id", "ANTIGRAVITY", "name", "Google Antigravity", "supportedModes", List.of("MANUAL", "ASSISTED"), "automatedCliVerified", false, "contextFiles", List.of("AGENTS.md", ".agents/rules/")),
                Map.of("id", "CLAUDE_CODE", "name", "Claude Code", "supportedModes", List.of("MANUAL", "ASSISTED", "AUTOMATED"), "automatedCliVerified", false, "cliBinary", "claude", "contextFiles", List.of("CLAUDE.md", "AGENTS.md")),
                Map.of("id", "OPENAI_CODEX", "name", "OpenAI Codex CLI", "supportedModes", List.of("MANUAL", "ASSISTED", "AUTOMATED"), "automatedCliVerified", false, "cliBinary", "codex", "contextFiles", List.of("AGENTS.md")),
                Map.of("id", "GITHUB_COPILOT", "name", "GitHub Copilot", "supportedModes", List.of("MANUAL", "ASSISTED"), "automatedCliVerified", false, "contextFiles", List.of("AGENTS.md")),
                Map.of("id", "GENERIC", "name", "Generic AI Coding Assistant", "supportedModes", List.of("MANUAL", "ASSISTED"), "automatedCliVerified", false, "contextFiles", List.of("AGENTS.md"))
        );
    }

    public CodingAgentExecution createAgentExecution(String projectId, AuthenticatedUser user, CodingAgentExecution req) {
        projectAuth.requireEditor(projectId, user);
        req.setId(null);
        req.setProjectId(projectId);
        req.setCreatedBy(user.email());
        String mode = req.getIntegrationMode() == null ? "MANUAL" : req.getIntegrationMode().toUpperCase(Locale.ROOT);
        req.setIntegrationMode(mode);
        req.setState("AUTOMATED".equals(mode) ? "WAITING_FOR_APPROVAL" : "PENDING");
        return agentExecutionRepository.save(req);
    }

    public CodingAgentExecution transitionAgentExecution(String projectId, String execId, AuthenticatedUser user, String newState, String report, List<String> modifiedFiles) {
        projectAuth.requireEditor(projectId, user);
        CodingAgentExecution exec = agentExecutionRepository.findById(execId)
                .filter(e -> e.getProjectId().equals(projectId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EXECUTION_NOT_FOUND", "Coding agent execution not found."));
        String targetState = newState.trim().toUpperCase(Locale.ROOT);
        // An agent completion report is a claim requiring verification!
        if ("COMPLETED".equals(targetState)) {
            targetState = "VERIFICATION_REQUIRED";
        }
        exec.setState(targetState);
        if (report != null) exec.setImportedReport(report);
        if (modifiedFiles != null) exec.setModifiedFiles(modifiedFiles);
        return agentExecutionRepository.save(exec);
    }

    public List<CodingAgentExecution> listAgentExecutions(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return agentExecutionRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    // =========================================================================
    // 5. EVIDENCE-BASED TRACEABILITY & STALE EVIDENCE INVALIDATION
    // =========================================================================

    public VerificationEvidence recordEvidence(String projectId, AuthenticatedUser user, VerificationEvidence req) {
        projectAuth.requireEditor(projectId, user);
        ProjectSpec spec = specRepository.findFirstByProjectIdOrderByVersionNumberDesc(projectId).orElse(null);

        req.setId(null);
        req.setProjectId(projectId);
        req.setSpecVersion(spec == null ? 0 : spec.getVersionNumber());
        req.setRecordedBy(user.email());
        req.setRecordedAt(clock.instant());

        // Distinguish evidence strength: FILE_EXISTS or AGENT_CLAIM alone NEVER proves VERIFIED!
        String strength = req.getEvidenceStrength() == null ? "AGENT_CLAIM" : req.getEvidenceStrength().toUpperCase(Locale.ROOT);
        req.setEvidenceStrength(strength);
        if ("FILE_EXISTS".equals(strength) || "AGENT_CLAIM".equals(strength)) {
            req.setVerificationStatus("CLAIM_UNVERIFIED");
        } else if ("FAILED".equalsIgnoreCase(req.getVerificationStatus())) {
            req.setVerificationStatus("FAILED");
        } else {
            req.setVerificationStatus("VERIFIED");
        }
        return evidenceRepository.save(req);
    }

    public int invalidateEvidenceForRequirement(String projectId, AuthenticatedUser user, String requirementId) {
        projectAuth.requireEditor(projectId, user);
        List<VerificationEvidence> list = evidenceRepository.findByProjectIdAndRequirementId(projectId, requirementId);
        int count = 0;
        for (var ev : list) {
            if ("VERIFIED".equals(ev.getVerificationStatus())) {
                ev.setVerificationStatus("STALE");
                evidenceRepository.save(ev);
                count++;
            }
        }
        return count;
    }

    public Map<String, Object> getTraceabilityMatrix(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        ProjectSpec spec = specRepository.findFirstByProjectIdOrderByVersionNumberDesc(projectId).orElse(null);
        PhasePlan plan = phasePlanRepository.findByProjectId(projectId).orElse(null);
        List<ProjectTask> tasks = taskRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
        List<VerificationEvidence> allEvidence = evidenceRepository.findByProjectIdOrderByRecordedAtDesc(projectId);

        List<ProjectSpec.RequirementItem> reqs = spec == null ? List.of() : spec.getFunctionalRequirements();
        int total = reqs.size();
        int planned = 0;
        int implemented = 0;
        int verified = 0;

        List<Map<String, Object>> rows = new ArrayList<>();
        for (var r : reqs) {
            List<String> phaseIds = plan == null ? List.of()
                    : plan.getPhases().stream().filter(p -> p.requirementIds().contains(r.id())).map(PhasePlan.PhaseItem::phaseId).toList();
            List<ProjectTask> linkedTasks = tasks.stream().filter(t -> t.getRequirementIds().contains(r.id())).toList();
            List<VerificationEvidence> linkedEv = allEvidence.stream().filter(e -> r.id().equals(e.getRequirementId())).toList();

            boolean isPlanned = !phaseIds.isEmpty() || !linkedTasks.isEmpty();
            boolean isVerified = linkedEv.stream().anyMatch(e -> "VERIFIED".equals(e.getVerificationStatus()));
            boolean isImplemented = isVerified || linkedTasks.stream().anyMatch(t -> "IMPLEMENTED".equals(t.getStatus()) || "VERIFIED".equals(t.getStatus()));

            if (isPlanned) planned++;
            if (isImplemented) implemented++;
            if (isVerified) verified++;

            String state = isVerified ? "VERIFIED" : (isImplemented ? "IMPLEMENTED" : (isPlanned ? "PLANNED" : "NOT_STARTED"));
            rows.add(Map.of(
                    "requirementId", r.id(),
                    "title", r.title(),
                    "state", state,
                    "phases", phaseIds,
                    "tasks", linkedTasks.stream().map(ProjectTask::getTaskCode).toList(),
                    "evidenceCount", linkedEv.size()
            ));
        }

        return Map.of(
                "totalRequirements", total,
                "planningCoveragePercent", total == 0 ? 0 : Math.round((planned * 100.0) / total),
                "implementationCoveragePercent", total == 0 ? 0 : Math.round((implemented * 100.0) / total),
                "verifiedCoveragePercent", total == 0 ? 0 : Math.round((verified * 100.0) / total),
                "rows", rows
        );
    }

    // =========================================================================
    // 6. ERROR FIX STUDIO (CLASSIFICATION + BOUNDED REPAIR ATTEMPTS)
    // =========================================================================

    public ErrorIncident diagnoseError(String projectId, AuthenticatedUser user, String title, String sourceType, String rawLog) {
        projectAuth.requireEditor(projectId, user);
        String log = rawLog == null ? "" : rawLog;
        String category = classifyError(log);
        String rootCause = explainRootCause(category);

        ErrorIncident inc = new ErrorIncident();
        inc.setProjectId(projectId);
        inc.setTitle(title == null || title.isBlank() ? category + " Error" : title.trim());
        inc.setSourceType(sourceType == null ? "TERMINAL" : sourceType.toUpperCase(Locale.ROOT));
        inc.setCategory(category);
        inc.setRawLogExcerpt(redactSecrets(log.substring(0, Math.min(log.length(), 4000))));
        inc.setAffectedComponent(inferComponent(log));
        inc.setLikelyRootCause(rootCause);
        inc.setRepairPrompt("Targeted Repair Prompt (" + category + "):\n1. Inspect affected component: " + inc.getAffectedComponent()
                + "\n2. Root cause hypothesis: " + rootCause
                + "\n3. Fix ONLY the root cause without disabling security or deleting tests.\n4. Re-run build & regression tests.");
        inc.setRollbackGuidance("Use `git diff` to review minimal changes; revert with `git checkout -- <file>` if regression tests fail.");
        inc.setStatus("DIAGNOSED");
        inc.setRepairAttempts(0);
        inc.setMaxRepairAttempts(3);
        return errorRepository.save(inc);
    }

    public ErrorIncident recordRepairAttempt(String projectId, String incidentId, AuthenticatedUser user, String fixMode, String summary, boolean verificationPassed) {
        projectAuth.requireEditor(projectId, user);
        ErrorIncident inc = errorRepository.findById(incidentId)
                .filter(e -> e.getProjectId().equals(projectId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INCIDENT_NOT_FOUND", "Error incident not found."));

        if (inc.getRepairAttempts() >= inc.getMaxRepairAttempts()) {
            inc.setStatus("BLOCKED");
            errorRepository.save(inc);
            throw new ApiException(HttpStatus.BAD_REQUEST, "MAX_REPAIR_ATTEMPTS_REACHED",
                    "Bounded repair limit (" + inc.getMaxRepairAttempts() + ") reached. Stopping to prevent an infinite repair loop — manual investigation required.");
        }

        int attempt = inc.getRepairAttempts() + 1;
        inc.setRepairAttempts(attempt);
        inc.getAttemptHistory().add(new ErrorIncident.RepairAttemptRecord(
                attempt, fixMode, summary, verificationPassed ? "PASS" : "FAIL", clock.instant()
        ));
        inc.setStatus(verificationPassed ? "VERIFIED_FIXED" : (attempt >= inc.getMaxRepairAttempts() ? "BLOCKED" : "REPAIR_ATTEMPTED"));
        return errorRepository.save(inc);
    }

    public List<ErrorIncident> listErrorIncidents(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return errorRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    private String classifyError(String log) {
        String l = log.toLowerCase(Locale.ROOT);
        if (l.contains("address already in use") || l.contains("eaddrinuse") || l.contains("port") && l.contains("already")) return "PORT_CONFLICT";
        if (l.contains("cannot find symbol") || l.contains("error ts") || l.contains("compilation failure") || l.contains("compile")) return "COMPILATION";
        if (l.contains("401") || l.contains("unauthorized") || l.contains("jwt")) return "AUTH";
        if (l.contains("403") || l.contains("accessdenied") || l.contains("forbidden")) return "AUTHORIZATION";
        if (l.contains("mongodb") || l.contains("sql") || l.contains("database") || l.contains("connection refused")) return "DATABASE";
        if (l.contains("renderflex overflowed") || l.contains("widget")) return "FRONTEND";
        if (l.contains("assertionerror") || l.contains("expected:") || l.contains("test failed")) return "TEST_FAILURE";
        if (l.contains("npm err") || l.contains("could not resolve dependencies")) return "DEPENDENCY";
        return "RUNTIME";
    }

    private String explainRootCause(String category) {
        return switch (category) {
            case "PORT_CONFLICT" -> "Target TCP port is already bound by another process.";
            case "COMPILATION" -> "Type mismatch, missing import, or invalid syntax detected during compilation.";
            case "AUTH" -> "Missing, expired, or invalid authentication token/credentials.";
            case "AUTHORIZATION" -> "Authenticated principal lacks required role or project membership.";
            case "DATABASE" -> "Database connection failure, schema mismatch, or missing replica/index configuration.";
            case "FRONTEND" -> "Layout constraint overflow or widget state rendering exception.";
            case "TEST_FAILURE" -> "Automated test assertion failed against current implementation.";
            case "DEPENDENCY" -> "Package resolution or incompatible transitive dependency version.";
            default -> "Unhandled runtime exception; inspect stack trace root frame.";
        };
    }

    private String inferComponent(String log) {
        if (log.contains(".java")) return "Backend (Java / Spring Boot)";
        if (log.contains(".dart")) return "Desktop (Flutter / Dart)";
        if (log.contains(".ts") || log.contains(".tsx")) return "Web / Admin (TypeScript / React)";
        if (log.contains(".py")) return "Python / ML Service";
        return "Application Core";
    }

    private boolean isStandaloneMl(Project project) {
        boolean hasMl = project.getArchetypes().stream().anyMatch(a -> a.toLowerCase(Locale.ROOT).contains("machine learning") || a.toLowerCase(Locale.ROOT).contains("data science"));
        boolean hasApp = project.getArchetypes().stream().anyMatch(a -> {
            String l = a.toLowerCase(Locale.ROOT);
            return l.contains("web") || l.contains("mobile") || l.contains("full-stack") || l.contains("e-commerce") || l.contains("api");
        });
        return hasMl && !hasApp;
    }

    private List<String> partitionSlice(List<String> items, int bucket, int totalBuckets) {
        if (items.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (i % totalBuckets == bucket) out.add(items.get(i));
        }
        return out;
    }

    private String redactSecrets(String s) {
        return s == null ? "" : SECRET_REDACT.matcher(s).replaceAll("$1=[REDACTED]");
    }
}
