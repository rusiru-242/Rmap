package com.rmap.platform.project;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ProjectRepository extends MongoRepository<Project, String> {
    @Query("{ '$or': [ { 'ownerId': ?0 }, { 'members.userId': ?0 } ] }")
    List<Project> findAccessibleByUserId(String userId);
}
