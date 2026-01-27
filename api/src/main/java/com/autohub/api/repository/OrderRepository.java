package com.autohub.api.repository;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderStatus;
import com.autohub.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Fetches a customer's order history sorted by most recent first.
     * Used in OrderService.findOrdersByUser
     */
    List<Order> findByUserOrderByOrderDateDesc(User user);

    /**
     * Silicon Valley Grade: Review Eligibility Logic
     * Ensures a user can only review a part if they have a DELIVERED or COMPLETED
     * order containing that specific part ID.
     */
    @Query("""
        SELECT COUNT(o) > 0
        FROM Order o
        JOIN o.items i
        WHERE o.user.id = :userId
          AND i.part.id = :partId
          AND o.status IN (:delivered, :completed)
    """)
    boolean existsEligibleDeliveredOrder(
            @Param("userId") Long userId,
            @Param("partId") Long partId,
            @Param("delivered") OrderStatus delivered,
            @Param("completed") OrderStatus completed
    );

    /**
     * Counts orders by status for Admin Dashboard metrics.
     */
    long countByStatus(OrderStatus status);
}