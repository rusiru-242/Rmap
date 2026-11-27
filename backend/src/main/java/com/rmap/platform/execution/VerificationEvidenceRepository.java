package com.rmap.platform.execution;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface VerificationEvidenceRepository extends MongoRepository<VerificationEvidence, String> {
    List<VerificationEvidence> findByProjectIdOrderByRecordedAtDesc(String projectId);
    List<VerificationEvidence> findByProjectIdAndRequirementId(String projectId, String requirementId);
}
