package com.rmap.platform.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rmap.platform.ai.AiUsageRecord;
import com.rmap.platform.ai.AiUsageRecordRepository;
import com.rmap.platform.ai.LlmProvider;
import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.document.ProjectDocument;
import com.rmap.platform.document.ProjectDocumentRepository;
import com.rmap.platform.project.Project;
import com.rmap.platform.project.ProjectAuthorizationService;
import com.rmap.platform.spec.ProjectSpec;
import com.rmap.platform.spec.ProjectSpecRepository;
import com.rmap.platform.spec.ProjectSpecService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProjectChatService {
    private final ChatConversationRepository chatRepository;
    private final ProjectAuthorizationService projectAuth;
    private final ProjectSpecRepository specRepository;
    private final ProjectSpecService specService;
    private final ProjectDocumentRepository documentRepository;
    private final LlmProvider llmProvider;
    private final AiUsageRecordRepository usageRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ProjectChatService(
            ChatConversationRepository chatRepository,
            ProjectAuthorizationService projectAuth,
            ProjectSpecRepository specRepository,
            ProjectSpecService specService,
            ProjectDocumentRepository documentRepository,
            LlmProvider llmProvider,
            AiUsageRecordRepository usageRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.chatRepository = chatRepository;
        this.projectAuth = projectAuth;
        this.specRepository = specRepository;
        this.specService = specService;
        this.documentRepository = documentRepository;
        this.llmProvider = llmProvider;
        this.usageRepository = usageRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public ChatConversation createConversation(String projectId, AuthenticatedUser user, String title) {
        projectAuth.requireMember(projectId, user);
        ChatConversation conv = new ChatConversation();
        conv.setProjectId(projectId);
        conv.setUserId(user.userId());
        conv.setTitle(title == null || title.isBlank() ? "New Project Session" : title.trim());
        return chatRepository.save(conv);
    }

    public List<ChatConversation> listConversations(String projectId, AuthenticatedUser user, String query) {
        projectAuth.requireMember(projectId, user);
        return chatRepository.findByProjectIdAndUserIdOrderByUpdatedAtDesc(projectId, user.userId()).stream()
                .filter(c -> query == null || query.isBlank()
                        || c.getTitle().toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT)))
                .toList();
    }

    public ChatConversation renameConversation(String projectId, String conversationId, AuthenticatedUser user, String newTitle) {
        ChatConversation conv = requireOwnedConversation(projectId, conversationId, user);
        conv.setTitle(newTitle.trim());
        return chatRepository.save(conv);
    }

    public void deleteConversation(String projectId, String conversationId, AuthenticatedUser user) {
        ChatConversation conv = requireOwnedConversation(projectId, conversationId, user);
        chatRepository.delete(conv);
    }

    public ChatConversation sendMessage(String projectId, String conversationId, AuthenticatedUser user, String userMessage) {
        Project project = projectAuth.requireMember(projectId, user);
        ChatConversation conv = requireOwnedConversation(projectId, conversationId, user);

        String context = buildStrictProjectContext(project, conv);
        String systemInstruction = """
                You are Rmap Project-Aware AI Assistant. Answer strictly using the authorized context of THIS project only.
                Do not ask the user for information already provided in the project context.
                Return ONLY valid JSON:
                {
                  "answer": "clear engineering answer",
                  "sourceRefs": ["REQ-001", "DocumentName p.1"],
                  "suggestedActions": ["action 1"],
                  "proposedSpecChange": null or {"targetSection":"functionalRequirements","summary":"...","title":"...","description":"..."}
                }
                """;

        LlmProvider.LlmResponse llmRes = llmProvider.generateJson(
                new LlmProvider.LlmRequest(systemInstruction, context + "\nUser Question: " + userMessage, 0.25, 2048)
        );

        try {
            JsonNode root = objectMapper.readTree(llmRes.rawJson());
            String answer = root.path("answer").asText("");
            if (answer.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "INVALID_AI_CHAT_RESPONSE", "AI assistant returned an empty answer.");
            }
            List<String> refs = readStringArray(root.path("sourceRefs"));
            List<String> actions = readStringArray(root.path("suggestedActions"));
            ChatConversation.SpecChangeProposal proposal = null;
            JsonNode propNode = root.path("proposedSpecChange");
            if (propNode.isObject() && !propNode.path("summary").asText("").isBlank()) {
                proposal = new ChatConversation.SpecChangeProposal(
                        UUID.randomUUID().toString(),
                        propNode.path("targetSection").asText("functionalRequirements"),
                        propNode.path("summary").asText(),
                        propNode.path("title").asText("Proposed Requirement"),
                        propNode.path("description").asText(""),
                        "PENDING_APPROVAL"
                );
            }

            conv.getMessages().add(new ChatConversation.ChatMessage(
                    UUID.randomUUID().toString(), "USER", userMessage.trim(), List.of(), List.of(), null, clock.instant()
            ));
            conv.getMessages().add(new ChatConversation.ChatMessage(
                    UUID.randomUUID().toString(), "ASSISTANT", answer, refs, actions, proposal, clock.instant()
            ));

            usageRepository.save(new AiUsageRecord(
                    user.userId(), projectId, conversationId, "PROJECT_CHAT",
                    llmRes.model(), llmRes.promptTokens(), llmRes.completionTokens(),
                    llmRes.estimatedCostUsd(), llmRes.latencyMs(), clock.instant()
            ));

            return chatRepository.save(conv);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "INVALID_AI_CHAT_RESPONSE", "Failed to validate AI chat JSON: " + ex.getMessage());
        }
    }

    public ProjectSpec approveSpecProposal(String projectId, String conversationId, String proposalId, AuthenticatedUser user) {
        projectAuth.requireEditor(projectId, user);
        ChatConversation conv = requireOwnedConversation(projectId, conversationId, user);

        ChatConversation.SpecChangeProposal found = null;
        List<ChatConversation.ChatMessage> updated = new ArrayList<>();
        for (var msg : conv.getMessages()) {
            if (msg.specProposal() != null && msg.specProposal().proposalId().equals(proposalId)) {
                found = msg.specProposal();
                var approvedProp = new ChatConversation.SpecChangeProposal(
                        found.proposalId(), found.targetSection(), found.summary(),
                        found.proposedRequirementTitle(), found.proposedRequirementDescription(), "APPROVED"
                );
                updated.add(new ChatConversation.ChatMessage(
                        msg.id(), msg.role(), msg.content(), msg.sourceRefs(), msg.suggestedActions(), approvedProp, msg.timestamp()
                ));
            } else {
                updated.add(msg);
            }
        }
        if (found == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PROPOSAL_NOT_FOUND", "Specification proposal not found.");
        }

        ProjectSpec latest = specService.getLatestOrInitDraft(projectId, user);
        List<ProjectSpec.RequirementItem> reqs = new ArrayList<>(latest.getFunctionalRequirements());
        String nextId = String.format("REQ-%03d", reqs.size() + 1);
        reqs.add(new ProjectSpec.RequirementItem(
                nextId,
                found.proposedRequirementTitle(),
                found.proposedRequirementDescription(),
                "MEDIUM",
                "AI Chat Proposal (" + conversationId + ")",
                true,
                true,
                List.of()
        ));
        latest.setFunctionalRequirements(reqs);
        ProjectSpec savedSpec = specService.saveDraft(projectId, user, latest);

        conv.setMessages(updated);
        chatRepository.save(conv);
        return savedSpec;
    }

    private ChatConversation requireOwnedConversation(String projectId, String conversationId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return chatRepository.findById(conversationId)
                .filter(c -> c.getProjectId().equals(projectId) && c.getUserId().equals(user.userId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Conversation not found in this project."));
    }

    private String buildStrictProjectContext(Project project, ChatConversation conv) {
        StringBuilder sb = new StringBuilder();
        sb.append("<AUTHORIZED_PROJECT_CONTEXT projectId=\"").append(project.getId()).append("\">\n");
        sb.append("Name: ").append(project.getName()).append(" | Mode: ").append(project.getMode()).append("\n");
        sb.append("Archetypes: ").append(project.getArchetypes()).append("\n");
        sb.append("Description: ").append(project.getDescription()).append("\n");
        sb.append("Mandatory Constraints: ").append(project.getMandatoryConstraints()).append("\n");

        specRepository.findFirstByProjectIdOrderByVersionNumberDesc(project.getId()).ifPresent(spec -> {
            sb.append("Active Spec v").append(spec.getVersionNumber()).append(" (").append(spec.getStatus()).append(")\n");
            sb.append("Architecture: ").append(spec.getArchitectureStyle()).append("\n");
            sb.append("Functional Requirements: ").append(spec.getFunctionalRequirements().size()).append("\n");
            for (int i = 0; i < Math.min(spec.getFunctionalRequirements().size(), 15); i++) {
                var r = spec.getFunctionalRequirements().get(i);
                sb.append("  - ").append(r.id()).append(": ").append(r.title()).append("\n");
            }
        });

        List<ProjectDocument> docs = documentRepository.findByProjectIdOrderByCreatedAtDesc(project.getId());
        if (!docs.isEmpty()) {
            sb.append("Documents:\n");
            for (var d : docs) {
                sb.append("  - ").append(d.getOriginalFileName()).append(": ").append(d.getExtractedSummary()).append("\n");
            }
        }
        sb.append("Recent Chat Turns: ").append(Math.min(conv.getMessages().size(), 6)).append("\n");
        sb.append("</AUTHORIZED_PROJECT_CONTEXT>");
        return sb.toString();
    }

    private List<String> readStringArray(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode n : node) {
                if (!n.asText("").isBlank()) out.add(n.asText());
            }
        }
        return out;
    }
}
