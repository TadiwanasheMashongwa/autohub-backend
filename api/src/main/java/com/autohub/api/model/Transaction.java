package com.autohub.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    private String paymentGateway; // e.g., "Paynow", "Stripe", "Cash"

    @Column(unique = true)
    private String gatewayReference; // The ID provided by the payment provider

    private BigDecimal amount;
    private String currency; // e.g., "USD", "ZiG"

    private String status; // SUCCESS, FAILED, PENDING

    private LocalDateTime timestamp;

    public Transaction() {
        this.timestamp = LocalDateTime.now();
    }

    public Transaction(Order order, String gateway, String reference, BigDecimal amount, String currency, String status) {
        this();
        this.order = order;
        this.paymentGateway = gateway;
        this.gatewayReference = reference;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    // --- GETTERS ---
    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public String getPaymentGateway() { return paymentGateway; }
    public String getGatewayReference() { return gatewayReference; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // --- SETTERS ---
    public void setId(Long id) { this.id = id; }
    public void setOrder(Order order) { this.order = order; }
    public void setPaymentGateway(String paymentGateway) { this.paymentGateway = paymentGateway; }
    public void setGatewayReference(String gatewayReference) { this.gatewayReference = gatewayReference; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setStatus(String status) { this.status = status; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}