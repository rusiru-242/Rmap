package com.rmap.platform.delivery;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DeploymentPlanRepository extends MongoRepository<DeploymentPlan, String> {
    List<DeploymentPlan> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
