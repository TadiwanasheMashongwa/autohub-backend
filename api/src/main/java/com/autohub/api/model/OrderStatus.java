package com.autohub.api.model;

public enum OrderStatus {
    PENDING,
    PAID,
    PICKED, // NEW: Physical verification complete
    SHIPPED,
    IN_TRANSIT,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED,
    REFUNDED
}