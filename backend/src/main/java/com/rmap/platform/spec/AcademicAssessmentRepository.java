package com.rmap.platform.spec;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AcademicAssessmentRepository extends MongoRepository<AcademicAssessment, String> {
    Optional<AcademicAssessment> findByProjectId(String projectId);
}
