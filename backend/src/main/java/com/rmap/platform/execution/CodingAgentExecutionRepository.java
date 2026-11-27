package com.rmap.platform.execution;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CodingAgentExecutionRepository extends MongoRepository<CodingAgentExecution, String> {
    List<CodingAgentExecution> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
