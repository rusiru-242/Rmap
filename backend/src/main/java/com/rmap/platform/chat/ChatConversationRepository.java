package com.rmap.platform.chat;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatConversationRepository extends MongoRepository<ChatConversation, String> {
    List<ChatConversation> findByProjectIdAndUserIdOrderByUpdatedAtDesc(String projectId, String userId);
}
