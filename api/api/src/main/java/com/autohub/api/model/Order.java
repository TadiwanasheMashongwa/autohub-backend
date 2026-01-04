package com.autohub.api.model;

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
    private User user;

    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private BigDecimal totalAmount;
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private BigDecimal refundedAmount = BigDecimal.ZERO;
    private String returnReason;
    private String couponCode;

    private String trackingNumber;
    private String courierName;
    private LocalDateTime shippedDate;

    private String paymentId;
    private String paymentStatus;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
        this.orderDate = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
    }

    // --- GETTERS ---
    public Long getId() { return id; }
    public User getUser() { return user; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public String getCouponCode() { return couponCode; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public String getReturnReason() { return returnReason; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getCourierName() { return courierName; }
    public LocalDateTime getShippedDate() { return shippedDate; }
    public String getPaymentId() { return paymentId; }
    public String getPaymentStatus() { return paymentStatus; }

    // --- SETTERS ---
    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public void setCourierName(String courierName) { this.courierName = courierName; }
    public void setShippedDate(LocalDateTime shippedDate) { this.shippedDate = shippedDate; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
}