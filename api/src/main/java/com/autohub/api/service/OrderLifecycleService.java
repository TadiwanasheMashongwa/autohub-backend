package com.autohub.api.service;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderStatus;
import com.autohub.api.model.User;
import com.autohub.api.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

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
     * Phase 6: Payment Confirmation Handshake
     * Logic used by PaymentController to finalize the Stripe transaction.
     */
    @Transactional
    public Order markPaid(Long orderId, String paymentId) {
        Order order = get(orderId);

        if ("PAID".equals(order.getPaymentStatus())) {
            return order;
        }

        order.setPaymentId(paymentId);
        order.setPaymentStatus("PAID");
        order.setStatus(OrderStatus.PAID);

        // Deduct items from physical inventory now that cash is confirmed
        inventoryService.deductReservedInventory(order);

        Order saved = orderRepository.save(order);

        try {
            emailService.sendOrderConfirmation(saved);
        } catch (Exception e) {
            System.err.println("Email failed, but transaction is safe: " + e.getMessage());
        }

        return saved;
    }

    /**
     * Phase 5: Logistics Override
     * Allows admins to manually correct tracking or courier data.
     */
    @Transactional
    public Order overrideLogistics(Long orderId, String courier, String tracking) {
        Order order = get(orderId);
        order.setCourierName(courier);
        order.setTrackingNumber(tracking);
        return orderRepository.save(order);
    }

    /**
     * Phase 5: Financial Settlement (Refund)
     * Triggers the refund status and releases inventory back to stock.
     */
    @Transactional
    public Order executeRefund(Long orderId) {
        Order order = get(orderId);
        order.setRefundedAmount(order.getTotalAmount());
        order.setStatus(OrderStatus.REFUNDED);
        order.setPaymentStatus("REFUNDED");

        // Return parts to available inventory
        inventoryService.releaseReservations(order);

        return orderRepository.save(order);
    }

    @Transactional
    public Order markShipped(Long orderId, String courier, String tracking) {
        Order order = get(orderId);
        order.setCourierName(courier);
        order.setTrackingNumber(tracking);
        order.setShippedDate(LocalDateTime.now());
        order.setStatus(OrderStatus.SHIPPED);
        return orderRepository.save(order);
    }

    private Order get(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order ID " + id + " not found in system."));
    }

    // Helper for review eligibility (Phase 4)
    public void assertCanReview(User user, Long partId) {
        boolean eligible = orderRepository.existsEligibleDeliveredOrder(
                user.getId(),
                partId,
                OrderStatus.DELIVERED,
                OrderStatus.COMPLETED
        );
        if (!eligible) throw new RuntimeException("Purchase verification failed for review.");
    }
}