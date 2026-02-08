package com.autohub.api.repository;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderStatus;
import com.autohub.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 🛠️ Standard lookup for Customer History
    List<Order> findByUserOrderByOrderDateDesc(User user);

    // 🛠️ Analytics logic for Admin Terminal Dashboard
    @Query("SELECT CAST(o.orderDate AS date) as date, SUM(o.totalAmount) as amount " +
            "FROM Order o WHERE o.status IN ('PAID', 'DELIVERED', 'COMPLETED', 'SHIPPED') " +
            "GROUP BY CAST(o.orderDate AS date) ORDER BY CAST(o.orderDate AS date) ASC")
    List<Map<String, Object>> getRevenueTrends();

    // 🛠️ Filters out cancelled/refunded orders for the Warehouse Queue
    @Query("SELECT DISTINCT o FROM Order o WHERE o.status NOT IN ('CANCELLED', 'REFUNDED') ORDER BY o.orderDate DESC")
    List<Order> findAllActiveOrders();

    // 🛠️ Simple status count for dashboard counters
    long countByStatus(OrderStatus status);

    // 🛠️ Verification for Review Eligibility
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
}