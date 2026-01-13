package com.autohub.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "part_id")
    private Part part;

    private Integer quantity; // Total quantity ordered

    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer pickedQuantity = 0; // Quantity actually scanned in warehouse

    private BigDecimal priceAtPurchase;

    public OrderItem() {}

    // --- GETTERS ---
    public Long getId() { return id; }
    public Part getPart() { return part; }
    public Integer getQuantity() { return quantity; }
    public Integer getPickedQuantity() { return pickedQuantity; }
    public BigDecimal getPriceAtPurchase() { return priceAtPurchase; }

    // --- SETTERS ---
    public void setId(Long id) { this.id = id; }
    public void setPart(Part part) { this.part = part; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setPickedQuantity(Integer pickedQuantity) { this.pickedQuantity = pickedQuantity; }
    public void setPriceAtPurchase(BigDecimal priceAtPurchase) { this.priceAtPurchase = priceAtPurchase; }
}