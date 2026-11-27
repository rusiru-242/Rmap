package com.rmap.platform.delivery;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DesignArtifactRepository extends MongoRepository<DesignArtifact, String> {
    List<DesignArtifact> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
