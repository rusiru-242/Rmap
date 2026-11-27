package com.rmap.platform.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmailDeliveryLogRepository extends MongoRepository<EmailDeliveryLog, String> {
    Page<EmailDeliveryLog> findAllByOrderBySentAtDesc(Pageable pageable);
}
