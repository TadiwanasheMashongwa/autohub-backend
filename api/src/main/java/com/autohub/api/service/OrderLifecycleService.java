package com.autohub.api.service;

import com.autohub.api.model.*;
import com.autohub.api.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class OrderLifecycleService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final EmailService emailService;

    public OrderLifecycleService(OrderRepository orderRepository,
                                 InventoryService inventoryService,
                                 EmailService emailService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.emailService = emailService;
    }

    /**
     * WAREHOUSE DISPATCH: Strict Barcode Verification (Checklist #1 & #4)
     * Enforces physical proof of stock before an order can move to PICKED status.
     */
    @Transactional
    public Order verifyAndPick(Long orderId, Map<String, String> verifyMap) {
        Order order = get(orderId);

        // Security Guardrail: Only PAID orders enter the picking mission
        if (order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("Picking Blocked: Order #" + orderId + " must be PAID before verification.");
        }

        // --- STRICT BARCODE AUDIT (Checklist #1) ---
        for (OrderItem item : order.getItems()) {
            String scanned = verifyMap.get(item.getId().toString());
            String actual = item.getPart().getBarcode();

            if (scanned == null || !scanned.equals(actual)) {
                throw new RuntimeException("Barcode Mismatch: Verification failed for item " + item.getPart().getName());
            }

            // Mark quantities as picked for internal tracking
            item.setPickedQuantity(item.getQuantity());
        }

        // Checklist #1 & #4: Timestamping and Status Transition
        order.setPickedDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PICKED);

        return orderRepository.save(order);
    }

    /**
     * LOGISTICS HANDSHAKE: Shipping Guardrails (Checklist #2)
     * Mandatory data entry before confirming shipment.
     */
    @Transactional
    public Order markShipped(Long orderId, String courier, String tracking) {
        Order order = get(orderId);

        // Checklist #2: Sequential Enforcement
        if (order.getStatus() != OrderStatus.PICKED) {
            throw new RuntimeException("Shipping Blocked: Order must be verified and PICKED before dispatch.");
        }

        // Checklist #2: Logistics Handshake
        if (courier == null || courier.trim().isEmpty() || tracking == null || tracking.trim().isEmpty()) {
            throw new RuntimeException("Shipping Blocked: Courier and Tracking Number are mandatory.");
        }

        order.setCourierName(courier);
        order.setTrackingNumber(tracking);
        order.setShippedDate(LocalDateTime.now());
        order.setStatus(OrderStatus.SHIPPED);

        Order saved = orderRepository.save(order);

        // Checklist #2: Auto-Notification
        try {
            emailService.sendShippingNotification(saved);
        } catch (Exception ignored) {}

        return saved;
    }

    /**
     * FINANCIAL SETTLEMENT: Move from PENDING to PAID
     */
    @Transactional
    public Order markPaid(Long orderId, String paymentId) {
        Order order = get(orderId);

        if (order.getStatus() == OrderStatus.PAID) return order;

        order.setPaymentId(paymentId);
        order.setPaymentStatus("PAID");
        order.setStatus(OrderStatus.PAID);

        // Checklist #4: Stock Locking (Finalizing the reservation)
        inventoryService.deductReservedInventory(order);

        Order saved = orderRepository.save(order);
        try {
            emailService.sendOrderConfirmation(saved);
        } catch (Exception ignored) {}

        return saved;
    }

    /**
     * ADMIN OVERRIDE: Logistics Correction
     * This method resolves the compilation error in AdminController.
     */
    @Transactional
    public Order overrideLogistics(Long orderId, String courier, String tracking) {
        Order order = get(orderId);

        if (courier != null) order.setCourierName(courier);
        if (tracking != null) order.setTrackingNumber(tracking);

        return orderRepository.save(order);
    }

    /**
     * DOMAIN ISOLATION: Admin-Only Refund Logic (Checklist #3)
     */
    @Transactional
    public Order executeRefund(Long orderId) {
        Order order = get(orderId);

        order.setRefundedAmount(order.getTotalAmount());
        order.setStatus(OrderStatus.REFUNDED);
        order.setPaymentStatus("REFUNDED");

        // Release reservations back to available stock
        inventoryService.releaseReservations(order);

        return orderRepository.save(order);
    }

    private Order get(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order ID " + id + " not found."));
    }

    public void assertCanReview(User user, Long partId) {
        boolean eligible = orderRepository.existsEligibleDeliveredOrder(
                user.getId(),
                partId,
                OrderStatus.DELIVERED,
                OrderStatus.COMPLETED
        );
        if (!eligible) throw new RuntimeException("Review blocked: Purchase and delivery required.");
    }
}