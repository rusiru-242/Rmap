package com.rmap.platform.spec;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DatasetProfileRepository extends MongoRepository<DatasetProfile, String> {
    List<DatasetProfile> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
