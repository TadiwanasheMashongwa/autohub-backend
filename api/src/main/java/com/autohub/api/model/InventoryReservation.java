package com.autohub.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "part_id")
    private Part part;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;

    public InventoryReservation() {
        this.createdAt = LocalDateTime.now();
    }

    public InventoryReservation(Part part, Order order, Integer quantity) {
        this();
        this.part = part;
        this.order = order;
        this.quantity = quantity;
    }

    // -------- GETTERS --------

    public Long getId() {
        return id;
    }

    public Part getPart() {
        return part;
    }

    public Order getOrder() {
        return order;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // -------- SETTERS --------

    public void setActive(boolean active) {
        this.active = active;
    }
}
