package com.rmap.platform.spec;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectSpecRepository extends MongoRepository<ProjectSpec, String> {
    List<ProjectSpec> findByProjectIdOrderByVersionNumberDesc(String projectId);
    Optional<ProjectSpec> findFirstByProjectIdOrderByVersionNumberDesc(String projectId);
    Optional<ProjectSpec> findByProjectIdAndVersionNumber(String projectId, int versionNumber);
}
