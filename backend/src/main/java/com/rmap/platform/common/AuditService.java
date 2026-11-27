package com.rmap.platform.common;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;

@Service
public class AuditService {
    private final AuditLogRepository repository;
    private final Clock clock;

    public AuditService(AuditLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(String actorId, String actorEmail, String action, String targetType, String targetId, Map<String, Object> metadata) {
        AuditLog entry = new AuditLog(
                actorId,
                actorEmail,
                action,
                targetType,
                targetId,
                metadata == null ? Map.of() : metadata,
                clock.instant()
        );
        repository.save(entry);
    }
}
