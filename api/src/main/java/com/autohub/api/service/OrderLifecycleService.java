package com.autohub.api.service;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderStatus;
import com.autohub.api.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class OrderLifecycleService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    public OrderLifecycleService(OrderRepository orderRepository, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * PHASE 5: Logistics Override.
     * Manually correct tracking info if a mistake was made during picking.
     */
    @Transactional
    public Order overrideLogistics(Long orderId, String courier, String tracking) {
        Order order = get(orderId);
        order.setCourierName(courier);
        order.setTrackingNumber(tracking);
        return orderRepository.save(order);
    }

    /**
     * PHASE 5: Financial Settlement.
     * Updates status and triggers inventory restock logic.
     */
    @Transactional
    public Order executeRefund(Long orderId) {
        Order order = get(orderId);

        // In a real Stripe integration, call stripe.refunds.create() here using order.getPaymentId()

        order.setRefundedAmount(order.getTotalAmount());
        order.setStatus(OrderStatus.REFUNDED);
        inventoryService.releaseReservations(order); // Return parts to stock

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
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
    }
}