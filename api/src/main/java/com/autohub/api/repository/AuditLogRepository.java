package com.autohub.api.repository;

import com.autohub.api.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * FIXED: Changed findByActionType to findByAction.
     * Aligned with 'action' field in AuditLog entity to resolve QueryCreationException.
     */
    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    /**
     * Matches the 'performedBy' field in the AuditLog entity.
     * Supports Admin tracking of specific Clerk activities.
     */
    List<AuditLog> findByPerformedByOrderByTimestampDesc(String performedBy);
}