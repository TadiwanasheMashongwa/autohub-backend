package com.autohub.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action; // e.g., "PRICE_CHANGE", "STOCK_ADJUSTMENT"
    private String performedBy; // Username of the Admin
    private String details;
    private LocalDateTime timestamp;

    public AuditLog() { this.timestamp = LocalDateTime.now(); }

    public AuditLog(String action, String performedBy, String details) {
        this();
        this.action = action;
        this.performedBy = performedBy;
        this.details = details;
    }

    // Getters
    public Long getId() { return id; }
    public String getAction() { return action; }
    public String getPerformedBy() { return performedBy; }
    public String getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }
}