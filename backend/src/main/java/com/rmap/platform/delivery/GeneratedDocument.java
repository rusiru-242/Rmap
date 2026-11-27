package com.rmap.platform.delivery;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "generated_documents")
public class GeneratedDocument {
    @Id
    private String id;
    @Indexed
    private String projectId;
    private int specVersion;
    private String docType; // SRS, SDD, ADR, API_DOCS, DB_DOCS, README, TEST_PLAN, SECURITY_DOC, TRACEABILITY_MATRIX, CLIENT_HANDOVER, ACADEMIC_DELIVERABLE_PACK, DEPLOYMENT_GUIDE, MAINTENANCE_GUIDE
    private String title;
    private int versionNumber;
    private String markdownContent;
    private String createdBy;
    @CreatedDate
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public int getSpecVersion() { return specVersion; }
    public void setSpecVersion(int specVersion) { this.specVersion = specVersion; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }
    public String getMarkdownContent() { return markdownContent; }
    public void setMarkdownContent(String markdownContent) { this.markdownContent = markdownContent; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
