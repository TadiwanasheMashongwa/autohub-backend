package com.autohub.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "applied_coupon_id")
    private Coupon appliedCoupon;

    public Cart() {}
    public Cart(User user) { this.user = user; }

    // --- LOGIC: Computed Fields ---

    public BigDecimal getTotalPrice() {
        return items.stream()
                .map(item -> item.getPart().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Integer getItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    // --- GETTERS & SETTERS ---
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
    public Coupon getAppliedCoupon() { return appliedCoupon; }
    public void setAppliedCoupon(Coupon appliedCoupon) { this.appliedCoupon = appliedCoupon; }
}