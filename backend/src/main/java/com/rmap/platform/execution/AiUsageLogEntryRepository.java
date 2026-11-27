package com.rmap.platform.execution;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AiUsageLogEntryRepository extends MongoRepository<AiUsageLogEntry, String> {
    List<AiUsageLogEntry> findByProjectIdOrderByTimestampDesc(String projectId);
}
