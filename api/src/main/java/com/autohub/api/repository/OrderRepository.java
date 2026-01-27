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

    List<Order> findByUserOrderByOrderDateDesc(User user);

    /**
     * PHASE 5: Revenue Time-Series.
     * Returns a list of dates and total revenue for that day.
     */
    @Query("SELECT CAST(o.orderDate AS date) as date, SUM(o.totalAmount) as amount " +
            "FROM Order o WHERE o.status IN ('PAID', 'DELIVERED', 'COMPLETED') " +
            "GROUP BY CAST(o.orderDate AS date) ORDER BY CAST(o.orderDate AS date) ASC")
    List<Map<String, Object>> getRevenueTrends();

    @Query("SELECT o FROM Order o WHERE o.status NOT IN ('CANCELLED', 'REFUNDED') ORDER BY o.orderDate DESC")
    List<Order> findAllActiveOrders();

    long countByStatus(OrderStatus status);
}