package com.rmap.platform.common;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "audit_logs")
public class AuditLog {
    @Id
    private String id;
    @Indexed
    private String actorId;
    private String actorEmail;
    @Indexed
    private String action;
    private String targetType;
    private String targetId;
    private Map<String, Object> metadata;
    @Indexed
    private Instant createdAt;

    public AuditLog() {}

    public AuditLog(String actorId, String actorEmail, String action, String targetType, String targetId, Map<String, Object> metadata, Instant createdAt) {
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getActorId() { return actorId; }
    public String getActorEmail() { return actorEmail; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
}
