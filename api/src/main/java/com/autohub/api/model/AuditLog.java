package com.autohub.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action; // Matches repository findByAction
    private String performedBy; // Matches repository findByPerformedBy
    private String details;
    private LocalDateTime timestamp;

    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }

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

    // Setters (Added for full entity support)
    public void setAction(String action) { this.action = action; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public void setDetails(String details) { this.details = details; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}