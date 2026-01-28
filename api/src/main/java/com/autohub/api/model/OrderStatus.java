package com.autohub.api.model;

public enum OrderStatus {
    PENDING,            // Initial state after checkout
    PAID,               // Payment confirmed. TRIGGER: Mission Mode active for Clerk.
    PICKED,             // Physical verification complete via Barcode (Checklist #1)
    SHIPPED,            // Logistics handshake complete (Checklist #2)
    IN_TRANSIT,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED,
    REFUNDED            // Admin-only financial reversal (Checklist #3)
}