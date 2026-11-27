package com.rmap.platform.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentTransactionRepository extends MongoRepository<PaymentTransaction, String> {
    boolean existsByProviderEventId(String providerEventId);
    Page<PaymentTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
