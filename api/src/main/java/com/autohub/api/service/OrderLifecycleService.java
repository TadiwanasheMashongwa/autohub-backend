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
     */
    @Transactional
    public Order verifyAndPick(Long orderId, Map<String, String> verifyMap) {
        Order order = get(orderId);

        // Security: Block picking unless payment confirmed
        if (order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("Picking Blocked: Order #" + orderId + " is not in PAID status.");
        }

        // --- STRICT BARCODE AUDIT ---
        for (OrderItem item : order.getItems()) {
            // Key is the OrderItem ID from the Picking Terminal
            String scanned = verifyMap.get(item.getId().toString());
            String actual = item.getPart().getBarcode();

            if (scanned == null || !scanned.equals(actual)) {
                throw new RuntimeException("Verification Failure: Barcode mismatch for " + item.getPart().getName());
            }

            // Mark quantities as picked for internal tracking
            item.setPickedQuantity(item.getQuantity());
        }

        // Checklist #1: Capture pickedDate for efficiency analytics
        order.setPickedDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PICKED);

        return orderRepository.save(order);
    }

    /**
     * LOGISTICS HANDSHAKE: Shipping Guardrails (Checklist #2)
     */
    @Transactional
    public Order markShipped(Long orderId, String courier, String tracking) {
        Order order = get(orderId);

        // Checklist #2: Sequential Enforcement (Must be PICKED first)
        if (order.getStatus() != OrderStatus.PICKED) {
            throw new RuntimeException("Shipping Blocked: Order must be verified and PICKED before dispatch.");
        }

        // Checklist #2: Mandatory Courier & Tracking entry
        if (courier == null || courier.trim().isEmpty() || tracking == null || tracking.trim().isEmpty()) {
            throw new RuntimeException("Shipping Blocked: Missing courier name or tracking number.");
        }

        order.setCourierName(courier);
        order.setTrackingNumber(tracking);
        order.setShippedDate(LocalDateTime.now());
        order.setStatus(OrderStatus.SHIPPED);

        Order saved = orderRepository.save(order);

        // Checklist #2: Auto-Notification after confirmation
        try {
            emailService.sendShippingNotification(saved);
        } catch (Exception e) {
            // Log error but don't roll back the transaction
        }

        return saved;
    }

    @Transactional
    public Order markPaid(Long orderId, String paymentId) {
        Order order = get(orderId);

        // Prevent double processing
        if (order.getStatus() == OrderStatus.PAID) return order;

        order.setPaymentId(paymentId);
        order.setPaymentStatus("PAID");
        order.setStatus(OrderStatus.PAID);

        // Checklist #4: Stock Locking (Confirming reserved inventory)
        inventoryService.deductReservedInventory(order);

        Order saved = orderRepository.save(order);
        try {
            emailService.sendOrderConfirmation(saved);
        } catch (Exception ignored) {}

        return saved;
    }

    @Transactional
    public Order executeRefund(Long orderId) {
        Order order = get(orderId);

        // Checklist #3: Role Governance - Handled by Security/Controller
        order.setRefundedAmount(order.getTotalAmount());
        order.setStatus(OrderStatus.REFUNDED);
        order.setPaymentStatus("REFUNDED");

        // Release any reserved stock
        inventoryService.releaseReservations(order);

        return orderRepository.save(order);
    }

    private Order get(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order ID " + id + " not found."));
    }

    public void assertCanReview(User user, Long partId) {
        boolean eligible = orderRepository.existsEligibleDeliveredOrder(user.getId(), partId, OrderStatus.DELIVERED, OrderStatus.COMPLETED);
        if (!eligible) throw new RuntimeException("Review blocked: Item not yet delivered.");
    }
}