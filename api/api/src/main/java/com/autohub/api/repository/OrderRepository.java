package com.autohub.api.repository;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderStatus;
import com.autohub.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Order entities.
 * Updated 2026.01.05: Fixed Review Verification Logic.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    List<Order> findByStatus(OrderStatus status);

    /**
     * Verifies if a user has a COMPLETED order containing a specific part.
     * Fixed: Changed status check from 'DELIVERED' to 'COMPLETED' to match current workflow.
     */
    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.items i " +
            "WHERE o.user = :user " +
            "AND i.part.id = :partId " +
            "AND o.status = com.autohub.api.model.OrderStatus.COMPLETED")
    boolean hasUserPurchasedPart(@Param("user") User user, @Param("partId") Long partId);
}