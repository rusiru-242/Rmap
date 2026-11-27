package com.rmap.platform.chat;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "chat_conversations")
public class ChatConversation {
    @Id
    private String id;
    @Indexed
    private String projectId;
    @Indexed
    private String userId;
    private String title;
    private List<ChatMessage> messages = new ArrayList<>();
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public record SpecChangeProposal(
            String proposalId,
            String targetSection,
            String summary,
            String proposedRequirementTitle,
            String proposedRequirementDescription,
            String status // PENDING_APPROVAL, APPROVED, REJECTED
    ) {}

    public record ChatMessage(
            String id,
            String role, // USER, ASSISTANT
            String content,
            List<String> sourceRefs,
            List<String> suggestedActions,
            SpecChangeProposal specProposal,
            Instant timestamp
    ) {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
