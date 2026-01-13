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
     * AUDIT #4.1: Retrieve a customer's personal order history.
     */
    List<Order> findByUser(User user);

    /**
     * PHASE 4 / AUDIT #9.1: Review Guard Logic.
     * Ensures only customers with a COMPLETED order for a specific part
     * can submit a review. Prevents fake ratings.
     */
    @Query("SELECT COUNT(o) > 0 FROM Order o " +
            "JOIN o.items i " +
            "WHERE o.user.id = :userId " +
            "AND i.part.id = :partId " +
            "AND o.status = com.autohub.api.model.OrderStatus.COMPLETED")
    boolean hasUserPurchasedPart(@Param("userId") Long userId, @Param("partId") Long partId);
}