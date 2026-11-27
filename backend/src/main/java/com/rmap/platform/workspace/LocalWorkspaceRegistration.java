package com.rmap.platform.workspace;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "local_workspaces")
public class LocalWorkspaceRegistration {
    @Id
    private String id;
    @Indexed
    private String userId;
    private String displayName;
    private String pathHash;
    private List<String> detectedStacks;
    private int indexedFileCount;
    private int excludedSensitiveFileCount;
    private boolean readOnlyMode;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public LocalWorkspaceRegistration() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPathHash() { return pathHash; }
    public void setPathHash(String pathHash) { this.pathHash = pathHash; }
    public List<String> getDetectedStacks() { return detectedStacks; }
    public void setDetectedStacks(List<String> detectedStacks) { this.detectedStacks = detectedStacks; }
    public int getIndexedFileCount() { return indexedFileCount; }
    public void setIndexedFileCount(int indexedFileCount) { this.indexedFileCount = indexedFileCount; }
    public int getExcludedSensitiveFileCount() { return excludedSensitiveFileCount; }
    public void setExcludedSensitiveFileCount(int excludedSensitiveFileCount) { this.excludedSensitiveFileCount = excludedSensitiveFileCount; }
    public boolean isReadOnlyMode() { return readOnlyMode; }
    public void setReadOnlyMode(boolean readOnlyMode) { this.readOnlyMode = readOnlyMode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
