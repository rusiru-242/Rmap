package com.rmap.platform.document;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectDocumentRepository extends MongoRepository<ProjectDocument, String> {
    List<ProjectDocument> findByProjectIdOrderByCreatedAtDesc(String projectId);
    Optional<ProjectDocument> findFirstByProjectIdAndSha256Hash(String projectId, String sha256Hash);
    long countByProjectIdAndOriginalFileNameIgnoreCase(String projectId, String originalFileName);
}
