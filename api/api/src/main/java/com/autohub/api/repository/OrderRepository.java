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
    List<Order> findByUser(User user);
    List<Order> findByStatus(OrderStatus status);

    /**
     * Verifies purchase using ID-based comparison.
     * This matches the Mike verification logic we are testing.
     */
    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.items i " +
            "WHERE o.user.id = :userId " +
            "AND i.part.id = :partId " +
            "AND o.status = com.autohub.api.model.OrderStatus.COMPLETED")
    boolean hasUserPurchasedPart(@Param("userId") Long userId, @Param("partId") Long partId);
}