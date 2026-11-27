package com.rmap.platform.ai;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WorkflowExecutionRepository extends MongoRepository<WorkflowExecution, String> {
    List<WorkflowExecution> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
