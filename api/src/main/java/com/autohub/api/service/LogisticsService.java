package com.autohub.api.service;

import com.autohub.api.model.AuditLog;
import com.autohub.api.model.Order;
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
     * Records logistics metadata ONLY.
     * Lifecycle transition is handled elsewhere.
     */
    @Transactional
    public Order attachShippingDetails(Long orderId, String courierName, String trackingNumber) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setCourierName(courierName);
        order.setTrackingNumber(trackingNumber);
        order.setShippedDate(LocalDateTime.now());

        auditLogRepository.save(new AuditLog(
                "LOGISTICS_ATTACHED",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "Courier " + courierName + " Tracking " + trackingNumber + " for Order #" + orderId
        ));

        return orderRepository.save(order);
    }
}
