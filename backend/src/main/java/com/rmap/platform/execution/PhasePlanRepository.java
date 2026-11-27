package com.rmap.platform.execution;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PhasePlanRepository extends MongoRepository<PhasePlan, String> {
    Optional<PhasePlan> findByProjectId(String projectId);
}
