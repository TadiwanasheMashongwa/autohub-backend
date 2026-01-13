package com.autohub.api.repository;

import com.autohub.api.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * AUDIT #11.4: Staff Performance Tracking.
     * Allows Mike to see all actions performed by a specific Clerk.
     */
    List<AuditLog> findByPerformedByOrderByTimestampDesc(String performedBy);

    /**
     * AUDIT #11.5: Event Investigation.
     * Filters logs by action type (e.g., 'STOCK_DEDUCTION', 'REFUND_ISSUED').
     */
    List<AuditLog> findByActionTypeOrderByTimestampDesc(String actionType);
}