package com.rmap.platform.execution;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProjectTaskRepository extends MongoRepository<ProjectTask, String> {
    List<ProjectTask> findByProjectIdOrderByCreatedAtAsc(String projectId);
    long countByProjectId(String projectId);
}
