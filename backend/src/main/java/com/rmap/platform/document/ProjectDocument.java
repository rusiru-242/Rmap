package com.rmap.platform.document;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "project_documents")
@CompoundIndex(name = "idx_doc_project_hash", def = "{'projectId': 1, 'sha256Hash': 1}")
public class ProjectDocument {
    @Id
    private String id;
    @Indexed
    private String projectId;
    private String uploadedBy;
    private String originalFileName;
    private String detectedType;
    private String mimeType;
    private long sizeBytes;
    private String sha256Hash;
    private int versionNumber;
    private String extractionStatus;
    private List<DocumentChunk> chunks = new ArrayList<>();
    private List<String> securityWarnings = new ArrayList<>();
    private String extractedSummary;
    @CreatedDate
    private Instant createdAt;

    public record DocumentChunk(
            int pageNumber,
            String sectionTitle,
            String sheetName,
            String text
    ) {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getDetectedType() { return detectedType; }
    public void setDetectedType(String detectedType) { this.detectedType = detectedType; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }
    public String getExtractionStatus() { return extractionStatus; }
    public void setExtractionStatus(String extractionStatus) { this.extractionStatus = extractionStatus; }
    public List<DocumentChunk> getChunks() { return chunks; }
    public void setChunks(List<DocumentChunk> chunks) { this.chunks = chunks; }
    public List<String> getSecurityWarnings() { return securityWarnings; }
    public void setSecurityWarnings(List<String> securityWarnings) { this.securityWarnings = securityWarnings; }
    public String getExtractedSummary() { return extractedSummary; }
    public void setExtractedSummary(String extractedSummary) { this.extractedSummary = extractedSummary; }
    public Instant getCreatedAt() { return createdAt; }
}
