package com.rmap.platform.ai;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface AiUsageRecordRepository extends MongoRepository<AiUsageRecord, String> {
    List<AiUsageRecord> findByUserIdAndCreatedAtAfter(String userId, Instant since);
    List<AiUsageRecord> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
