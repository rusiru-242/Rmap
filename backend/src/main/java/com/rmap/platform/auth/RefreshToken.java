package com.rmap.platform.auth;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "refresh_tokens")
public class RefreshToken {
    @Id
    private String id;
    @Indexed
    private String userId;
    @Indexed
    private String familyId;
    @Indexed(unique = true)
    private String tokenHash;
    private Instant expiresAt;
    private boolean revoked;
    private String replacedByTokenHash;
    private Instant createdAt;

    public RefreshToken() {}

    public RefreshToken(String userId, String familyId, String tokenHash, Instant expiresAt, Instant createdAt) {
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revoked = false;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getFamilyId() { return familyId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public String getReplacedByTokenHash() { return replacedByTokenHash; }
    public void setReplacedByTokenHash(String replacedByTokenHash) { this.replacedByTokenHash = replacedByTokenHash; }
    public Instant getCreatedAt() { return createdAt; }
}
