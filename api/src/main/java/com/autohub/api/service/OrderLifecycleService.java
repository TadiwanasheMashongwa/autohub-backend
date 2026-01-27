package com.autohub.api.service;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderStatus;
import com.autohub.api.model.User;
import com.autohub.api.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /* ---------------- PAYMENT (PHASE 6) ---------------- */

    @Transactional
    public Order markPaid(Long orderId, String paymentId) {
        Order order = get(orderId);

        if ("PAID".equals(order.getPaymentStatus())) {
            return order;
        }

        assertStatus(order, OrderStatus.PENDING);

        order.setPaymentId(paymentId);
        order.setPaymentStatus("PAID");
        order.setStatus(OrderStatus.PAID);

        inventoryService.deductReservedInventory(order);

        Order saved = orderRepository.save(order);

        try {
            emailService.sendOrderConfirmation(saved);
        } catch (Exception e) {
            System.err.println("Non-critical: Email confirmation failed: " + e.getMessage());
        }

        return saved;
    }

    /* ---------------- SHIPPING (PHASE 7 - LOGISTICS) ---------------- */

    /**
     * Silicon Valley Grade Logistics: Marks order as shipped and triggers notifications.
     * Matches AdminController:69 requirements.
     */
    @Transactional
    public Order markShipped(Long orderId, String courier, String tracking) {
        Order order = get(orderId);
        assertStatus(order, OrderStatus.PAID);

        // Validation: Ensure all items were picked before shipping
        boolean allPicked = order.getItems().stream()
                .allMatch(i -> i.getPickedQuantity() != null && i.getPickedQuantity().equals(i.getQuantity()));

        if (!allPicked) {
            throw new RuntimeException("Cannot ship order: items not fully picked in warehouse");
        }

        order.setCourierName(courier);
        order.setTrackingNumber(tracking);
        order.setStatus(OrderStatus.SHIPPED);

        Order saved = orderRepository.save(order);

        try {
            emailService.sendShippingNotification(saved);
        } catch (Exception e) {
            System.err.println("Non-critical: Shipping notification failed: " + e.getMessage());
        }

        return saved;
    }

    @Transactional
    public Order markInTransit(Long orderId) {
        Order order = get(orderId);
        assertStatus(order, OrderStatus.SHIPPED);

        order.setStatus(OrderStatus.IN_TRANSIT);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markDelivered(Long orderId) {
        Order order = get(orderId);
        assertStatus(order, OrderStatus.IN_TRANSIT);

        order.setStatus(OrderStatus.DELIVERED);
        Order saved = orderRepository.save(order);

        try {
            emailService.sendDeliveryConfirmation(saved);
        } catch (Exception e) {
            System.err.println("Non-critical: Delivery confirmation failed.");
        }

        return saved;
    }

    /* ---------------- RETURNS & REFUNDS ---------------- */

    @Transactional
    public Order markReturned(Long orderId) {
        Order order = get(orderId);
        // Can only return if it was actually delivered
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new RuntimeException("Order must be delivered or requested for return before marking as returned");
        }

        order.setStatus(OrderStatus.RETURNED);
        return orderRepository.save(order);
    }

    @Transactional
    public Order refund(Long orderId) {
        Order order = get(orderId);
        assertStatus(order, OrderStatus.RETURNED);

        // Release reservations/restock logic
        inventoryService.releaseReservations(order);

        order.setStatus(OrderStatus.REFUNDED);
        order.setPaymentStatus("REFUNDED");

        Order saved = orderRepository.save(order);

        try {
            emailService.sendRefundConfirmation(saved);
        } catch (Exception e) {
            System.err.println("Non-critical: Refund email failed.");
        }

        return saved;
    }

    /* ---------------- HELPERS ---------------- */

    private Order get(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    private void assertStatus(Order order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new RuntimeException(
                    "Illegal transition: expected " + expected +
                            " but was " + order.getStatus()
            );
        }
    }
}