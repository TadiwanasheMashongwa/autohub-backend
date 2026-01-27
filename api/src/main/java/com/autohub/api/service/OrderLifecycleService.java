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

    @Transactional
    public Order markPaid(Long orderId, String paymentId) {
        Order order = get(orderId);

        // Prevent double-processing
        if ("PAID".equals(order.getPaymentStatus())) {
            return order;
        }

        assertStatus(order, OrderStatus.PENDING);

        order.setPaymentId(paymentId);
        order.setPaymentStatus("PAID");
        order.setStatus(OrderStatus.PAID);

        // Atomic Inventory Deduction
        inventoryService.deductReservedInventory(order);

        Order saved = orderRepository.save(order);

        try {
            emailService.sendOrderConfirmation(saved);
        } catch (Exception e) {
            // Log email failure but don't roll back the payment success
            System.err.println("Email notification failed for order: " + orderId);
        }

        return saved;
    }

    /* Helper Methods remain same as provided */
    private Order get(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    private void assertStatus(Order order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new RuntimeException("Illegal transition: expected " + expected + " but was " + order.getStatus());
        }
    }

    // Other shipping/return methods remain as implemented in your previous version...
}