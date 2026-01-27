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

    @Transactional
    public Order markShipped(Long orderId, String courier, String tracking) {
        Order order = get(orderId);
        assertStatus(order, OrderStatus.PAID);

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
            System.err.println("Non-critical: Shipping notification failed.");
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
            System.err.println("Non-critical: Delivery notification failed.");
        }

        return saved;
    }

    /* ---------------- RETURNS & REFUNDS ---------------- */

    @Transactional
    public Order markReturned(Long orderId) {
        Order order = get(orderId);
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new RuntimeException("Order must be delivered/requested before marking as returned");
        }

        order.setStatus(OrderStatus.RETURNED);
        return orderRepository.save(order);
    }

    @Transactional
    public Order refund(Long orderId) {
        Order order = get(orderId);
        assertStatus(order, OrderStatus.RETURNED);

        inventoryService.releaseReservations(order);

        order.setStatus(OrderStatus.REFUNDED);
        order.setPaymentStatus("REFUNDED");

        return orderRepository.save(order);
    }

    /* ---------------- REVIEWS (PHASE 8) ---------------- */

    /**
     * FIXED: Added OrderStatus.COMPLETED to match the 4-argument signature
     * in OrderRepository.existsEligibleDeliveredOrder.
     */
    public void assertCanReview(User user, Long partId) {
        boolean eligible = orderRepository.existsEligibleDeliveredOrder(
                user.getId(),
                partId,
                OrderStatus.DELIVERED,
                OrderStatus.COMPLETED
        );

        if (!eligible) {
            throw new RuntimeException(
                    "Review blocked: Part not purchased or order not yet delivered/completed."
            );
        }
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