package com.autohub.api.repository;

import com.autohub.api.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {
    /**
     * Supports Phase 6: Reliability.
     * Stores unique Idempotency-Keys to prevent duplicate order creation
     * during checkout or payment processing.
     */
}