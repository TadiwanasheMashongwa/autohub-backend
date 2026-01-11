package com.autohub.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {
    @Id
    private String idempotencyKey; // The unique UUID from frontend

    @Column(columnDefinition = "TEXT")
    private String responseBody; // The cached JSON response

    private Integer statusCode;
    private LocalDateTime createdAt;

    public IdempotencyRecord() { this.createdAt = LocalDateTime.now(); }

    public IdempotencyRecord(String key, String body, Integer status) {
        this();
        this.idempotencyKey = key;
        this.responseBody = body;
        this.statusCode = status;
    }

    // Getters
    public String getResponseBody() { return responseBody; }
    public Integer getStatusCode() { return statusCode; }
}