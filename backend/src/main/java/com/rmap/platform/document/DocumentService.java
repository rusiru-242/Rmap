package com.rmap.platform.document;

import com.rmap.platform.auth.AuthenticatedUser;
import com.rmap.platform.common.ApiException;
import com.rmap.platform.common.AuditService;
import com.rmap.platform.project.ProjectAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class DocumentService {
    private final ProjectDocumentRepository repository;
    private final ProjectAuthorizationService projectAuth;
    private final SafeDocumentParser parser;
    private final AuditService auditService;

    public DocumentService(
            ProjectDocumentRepository repository,
            ProjectAuthorizationService projectAuth,
            SafeDocumentParser parser,
            AuditService auditService
    ) {
        this.repository = repository;
        this.projectAuth = projectAuth;
        this.parser = parser;
        this.auditService = auditService;
    }

    public ProjectDocument upload(String projectId, AuthenticatedUser user, String fileName, String mimeType, byte[] bytes) {
        projectAuth.requireEditor(projectId, user);
        SafeDocumentParser.ParsedDocumentResult parsed = parser.parse(fileName, bytes);
        String hash = sha256(bytes);

        var duplicate = repository.findFirstByProjectIdAndSha256Hash(projectId, hash);
        if (duplicate.isPresent()) {
            return duplicate.get();
        }

        int nextVersion = (int) repository.countByProjectIdAndOriginalFileNameIgnoreCase(projectId, parsed.sanitizedFileName()) + 1;
        ProjectDocument doc = new ProjectDocument();
        doc.setProjectId(projectId);
        doc.setUploadedBy(user.userId());
        doc.setOriginalFileName(parsed.sanitizedFileName());
        doc.setDetectedType(parsed.detectedType());
        doc.setMimeType(mimeType == null ? "application/octet-stream" : mimeType);
        doc.setSizeBytes(bytes.length);
        doc.setSha256Hash(hash);
        doc.setVersionNumber(nextVersion);
        doc.setExtractionStatus("EXTRACTED");
        doc.setChunks(parsed.chunks());
        doc.setSecurityWarnings(parsed.securityWarnings());
        doc.setExtractedSummary(parsed.summary());

        doc = repository.save(doc);
        auditService.record(user.userId(), user.email(), "DOCUMENT_UPLOADED", "PROJECT_DOCUMENT", doc.getId(), Map.of(
                "projectId", projectId,
                "fileName", doc.getOriginalFileName(),
                "type", doc.getDetectedType(),
                "version", doc.getVersionNumber()
        ));
        return doc;
    }

    public List<ProjectDocument> list(String projectId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return repository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public ProjectDocument get(String projectId, String documentId, AuthenticatedUser user) {
        projectAuth.requireMember(projectId, user);
        return repository.findById(documentId)
                .filter(d -> d.getProjectId().equals(projectId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document not found in this project."));
    }

    public void delete(String projectId, String documentId, AuthenticatedUser user) {
        projectAuth.requireEditor(projectId, user);
        ProjectDocument doc = get(projectId, documentId, user);
        repository.delete(doc);
        auditService.record(user.userId(), user.email(), "DOCUMENT_DELETED", "PROJECT_DOCUMENT", documentId, Map.of("projectId", projectId));
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
