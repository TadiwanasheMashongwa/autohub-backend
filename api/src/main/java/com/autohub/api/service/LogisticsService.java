package com.autohub.api.service;

import com.autohub.api.model.AuditLog;
import com.autohub.api.model.Order;
import com.autohub.api.model.OrderStatus;
import com.autohub.api.repository.AuditLogRepository;
import com.autohub.api.repository.OrderRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LogisticsService {

    private final OrderRepository orderRepository;
    private final AuditLogRepository auditLogRepository;

    public LogisticsService(OrderRepository orderRepository,
                            AuditLogRepository auditLogRepository) {
        this.orderRepository = orderRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * STEP 5.6.1 — Ship order
     * Preconditions:
     * - All items must be fully picked
     * - Order must not already be shipped
     */
    @Transactional
    public Order shipOrder(Long orderId, String courierName, String trackingNumber) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        boolean allPicked = order.getItems().stream()
                .allMatch(i -> i.getPickedQuantity().equals(i.getQuantity()));

        if (!allPicked) {
            throw new RuntimeException("Cannot ship order: not all items are picked");
        }

        order.setCourierName(courierName);
        order.setTrackingNumber(trackingNumber);
        order.setShippedDate(LocalDateTime.now());
        order.setStatus(OrderStatus.SHIPPED);

        auditLogRepository.save(new AuditLog(
                "ORDER_SHIPPED",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "Order #" + orderId + " shipped via " + courierName
        ));

        return orderRepository.save(order);
    }

    /**
     * STEP 5.6.2 — Move order to IN_TRANSIT
     */
    @Transactional
    public Order markInTransit(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new RuntimeException("Order must be SHIPPED before IN_TRANSIT");
        }

        order.setStatus(OrderStatus.IN_TRANSIT);

        auditLogRepository.save(new AuditLog(
                "ORDER_IN_TRANSIT",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "Order #" + orderId + " is now IN_TRANSIT"
        ));

        return orderRepository.save(order);
    }
}
