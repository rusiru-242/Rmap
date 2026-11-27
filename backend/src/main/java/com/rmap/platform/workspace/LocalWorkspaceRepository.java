package com.rmap.platform.workspace;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LocalWorkspaceRepository extends MongoRepository<LocalWorkspaceRegistration, String> {
    List<LocalWorkspaceRegistration> findByUserIdOrderByUpdatedAtDesc(String userId);
    Optional<LocalWorkspaceRegistration> findByUserIdAndPathHash(String userId, String pathHash);
}
