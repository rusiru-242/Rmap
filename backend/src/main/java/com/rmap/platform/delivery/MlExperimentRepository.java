package com.rmap.platform.delivery;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MlExperimentRepository extends MongoRepository<MlExperiment, String> {
    List<MlExperiment> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
