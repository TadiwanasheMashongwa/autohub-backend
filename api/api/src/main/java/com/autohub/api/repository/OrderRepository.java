package com.autohub.api.repository;

import com.autohub.api.model.Order;
import com.autohub.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Allows us to fetch all orders for a specific customer
    List<Order> findByUser(User user);

    // Allows us to fetch orders by their current status (e.g., for Admin dashboard)
    List<Order> findByStatus(String status);
}