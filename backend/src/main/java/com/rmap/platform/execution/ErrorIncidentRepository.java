package com.rmap.platform.execution;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ErrorIncidentRepository extends MongoRepository<ErrorIncident, String> {
    List<ErrorIncident> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
