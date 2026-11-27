package com.rmap.platform.chat;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.billing.RequiresActiveEntitlement;
import com.rmap.platform.common.ApiResponse;
import com.rmap.platform.spec.ProjectSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/chat/conversations")
public class ProjectChatController {
    private final ProjectChatService chatService;

    public ProjectChatController(ProjectChatService chatService) {
        this.chatService = chatService;
    }

    public record CreateChatRequest(@Size(max = 120) String title) {}
    public record RenameChatRequest(@NotBlank @Size(max = 120) String title) {}
    public record SendMessageRequest(@NotBlank @Size(max = 4000) String message) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresActiveEntitlement
    public ApiResponse<ChatConversation> create(
            @PathVariable String projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody(required = false) CreateChatRequest req
    ) {
        return ApiResponse.ok(chatService.createConversation(projectId, user, req == null ? null : req.title()));
    }

    @GetMapping
    public ApiResponse<List<ChatConversation>> list(
            @PathVariable String projectId,
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(chatService.listConversations(projectId, user, q));
    }

    @PatchMapping("/{conversationId}")
    @RequiresActiveEntitlement
    public ApiResponse<ChatConversation> rename(
            @PathVariable String projectId,
            @PathVariable String conversationId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody RenameChatRequest req
    ) {
        return ApiResponse.ok(chatService.renameConversation(projectId, conversationId, user, req.title()));
    }

    @DeleteMapping("/{conversationId}")
    @RequiresActiveEntitlement
    public ApiResponse<Map<String, String>> delete(
            @PathVariable String projectId,
            @PathVariable String conversationId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        chatService.deleteConversation(projectId, conversationId, user);
        return ApiResponse.ok(Map.of("deleted", conversationId));
    }

    @PostMapping("/{conversationId}/messages")
    @RequiresActiveEntitlement
    public ApiResponse<ChatConversation> sendMessage(
            @PathVariable String projectId,
            @PathVariable String conversationId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody SendMessageRequest req
    ) {
        return ApiResponse.ok(chatService.sendMessage(projectId, conversationId, user, req.message()));
    }

    @PostMapping("/{conversationId}/proposals/{proposalId}/approve")
    @RequiresActiveEntitlement
    public ApiResponse<ProjectSpec> approveProposal(
            @PathVariable String projectId,
            @PathVariable String conversationId,
            @PathVariable String proposalId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(chatService.approveSpecProposal(projectId, conversationId, proposalId, user));
    }
}
