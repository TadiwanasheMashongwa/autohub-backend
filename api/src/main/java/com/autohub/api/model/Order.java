package com.autohub.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private BigDecimal totalAmount;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    private String couponCode;
    private String returnReason;

    private String trackingNumber;
    private String courierName;
    private LocalDateTime shippedDate;
    private LocalDateTime deliveryDate;

    private String paymentId;
    private String paymentStatus;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
        this.orderDate = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
    }

    // -------- GETTERS --------

    public Long getId() { return id; }
    public User getUser() { return user; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public String getCouponCode() { return couponCode; }
    public String getPaymentId() { return paymentId; }
    public String getPaymentStatus() { return paymentStatus; }
    public List<OrderItem> getItems() { return items; }

    // -------- SETTERS --------

    public void setUser(User user) { this.user = user; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
