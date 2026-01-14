package com.autohub.api.service;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderStatus;
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

    /* ---------------- PAYMENT ---------------- */

    @Transactional
    public Order markPaid(Long orderId, String paymentId) {
        Order order = get(orderId);
        assertStatus(order, OrderStatus.PENDING);

        order.setPaymentId(paymentId);
        order.setPaymentStatus("PAID");
        order.setStatus(OrderStatus.PAID);

        // Inventory deducted EXACTLY ONCE here
        inventoryService.deductReservedInventory(order);

        Order saved = orderRepository.save(order);
        emailService.sendOrderConfirmation(saved);
        return saved;
    }

    /* ---------------- SHIPPING ---------------- */

    @Transactional
    public Order markShipped(Long orderId, String courier, String tracking) {
        Order order = get(orderId);
        assertStatus(order, OrderStatus.PAID);

        boolean allPicked = order.getItems().stream()
                .allMatch(i -> i.getPickedQuantity().equals(i.getQuantity()));

        if (!allPicked) {
            throw new RuntimeException("Cannot ship order: not all items are fully picked");
        }

        order.setCourierName(courier);
        order.setTrackingNumber(tracking);
        order.setStatus(OrderStatus.SHIPPED);

        Order saved = orderRepository.save(order);
        emailService.sendShippingNotification(saved);
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
        emailService.sendDeliveryConfirmation(saved);
        return saved;
    }

    @Transactional
    public Order complete(Long orderId) {
        Order order = get(orderId);
        assertStatus(order, OrderStatus.DELIVERED);

        order.setStatus(OrderStatus.COMPLETED);
        return orderRepository.save(order);
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
