package com.autohub.api.repository;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

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
