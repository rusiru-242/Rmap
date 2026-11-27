package com.rmap.platform.execution;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MasterPromptVersionRepository extends MongoRepository<MasterPromptVersion, String> {
    List<MasterPromptVersion> findByProjectIdOrderByCreatedAtDesc(String projectId);
    long countByProjectId(String projectId);
}
