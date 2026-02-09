package com.autohub.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    @ManyToOne(fetch = FetchType.EAGER) // 🛠️ Eager load for dashboard speed
    @JoinColumn(name = "user_id", nullable = false)
    // 🛠️ FIXED: Removed @JsonIgnore. Using properties to block recursion but allow basic info.
    @JsonIgnoreProperties({"orders", "password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled"})
    private User user;

    private LocalDateTime orderDate;
    private LocalDateTime pickedDate;
    private LocalDateTime shippedDate;
    private LocalDateTime deliveryDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private BigDecimal totalAmount;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    private String couponCode;
    private String trackingNumber;
    private String courierName;
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
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public String getCouponCode() { return couponCode; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getCourierName() { return courierName; }
    public LocalDateTime getPickedDate() { return pickedDate; }
    public LocalDateTime getShippedDate() { return shippedDate; }
    public LocalDateTime getDeliveryDate() { return deliveryDate; }
    public String getPaymentId() { return paymentId; }
    public String getPaymentStatus() { return paymentStatus; }
    public List<OrderItem> getItems() { return items; }
    public LocalDateTime getOrderDate() { return orderDate; }

    // --- SETTERS ---
    public void setUser(User user) { this.user = user; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public void setCourierName(String courierName) { this.courierName = courierName; }
    public void setPickedDate(LocalDateTime pickedDate) { this.pickedDate = pickedDate; }
    public void setShippedDate(LocalDateTime shippedDate) { this.shippedDate = shippedDate; }
    public void setDeliveryDate(LocalDateTime deliveryDate) { this.deliveryDate = deliveryDate; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
}