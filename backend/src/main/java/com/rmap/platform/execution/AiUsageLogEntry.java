package com.rmap.platform.execution;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "ai_usage_logs")
public class AiUsageLogEntry {
    @Id
    private String id;
    @Indexed
    private String projectId;
    private String userEmail;
    private String eventType; // PROMPT_GENERATED, PROMPT_COPIED, AGENT_EXECUTED, SUGGESTION_REJECTED
    private String targetTool;
    private String modelInfo;
    private String purpose;
    private String requirementRef;
    private String promptExcerptRedacted;
    private String userVerificationNotes;
    private Instant timestamp;

    public AiUsageLogEntry() {}

    public AiUsageLogEntry(
            String projectId,
            String userEmail,
            String eventType,
            String targetTool,
            String modelInfo,
            String purpose,
            String requirementRef,
            String promptExcerptRedacted,
            String userVerificationNotes,
            Instant timestamp
    ) {
        this.projectId = projectId;
        this.userEmail = userEmail;
        this.eventType = eventType;
        this.targetTool = targetTool;
        this.modelInfo = modelInfo;
        this.purpose = purpose;
        this.requirementRef = requirementRef;
        this.promptExcerptRedacted = promptExcerptRedacted;
        this.userVerificationNotes = userVerificationNotes;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getUserEmail() { return userEmail; }
    public String getEventType() { return eventType; }
    public String getTargetTool() { return targetTool; }
    public String getModelInfo() { return modelInfo; }
    public String getPurpose() { return purpose; }
    public String getRequirementRef() { return requirementRef; }
    public String getPromptExcerptRedacted() { return promptExcerptRedacted; }
    public String getUserVerificationNotes() { return userVerificationNotes; }
    public Instant getTimestamp() { return timestamp; }
}
