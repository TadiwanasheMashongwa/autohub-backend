package com.autohub.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime orderDate;
    private String status; // PENDING, COMPLETED, CANCELLED
    private Double totalAmount;

    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderItem> items;

    public Order() { this.orderDate = LocalDateTime.now(); }
    // Getters and Setters...
}