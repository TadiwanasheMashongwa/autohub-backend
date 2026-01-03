package com.autohub.api.service;

import com.autohub.api.model.*;
import com.autohub.api.repository.AuditLogRepository;
import com.autohub.api.repository.IdempotencyRepository;
import com.autohub.api.repository.OrderRepository;
import com.autohub.api.repository.PartRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PartRepository partRepository;
    private final EmailService emailService;
    private final IdempotencyRepository idempotencyRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        PartRepository partRepository,
                        EmailService emailService,
                        IdempotencyRepository idempotencyRepository,
                        AuditLogRepository auditLogRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.partRepository = partRepository;
        this.emailService = emailService;
        this.idempotencyRepository = idempotencyRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    // ... Existing checkoutCart and createOrderWithCoupon methods ...

    @Transactional
    public Order requestReturn(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new RuntimeException("Only completed orders can be returned.");
        }

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.setReturnReason(reason);
        return orderRepository.save(order);
    }

    @Transactional
    public Order processRefund(Long orderId, BigDecimal amount, boolean restockItems) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (amount.compareTo(order.getTotalAmount()) > 0) {
            throw new RuntimeException("Refund amount cannot exceed total order amount.");
        }

        order.setRefundedAmount(amount);
        order.setStatus(OrderStatus.REFUNDED);

        if (restockItems) {
            for (OrderItem item : order.getItems()) {
                Part part = item.getPart();
                // This utilizes our @Version Optimistic Locking automatically
                part.setStockQuantity(part.getStockQuantity() + item.getQuantity());
                partRepository.save(part);
            }
        }

        Order savedOrder = orderRepository.save(order);

        // Record for Audit
        String adminName = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogRepository.save(new AuditLog(
                "ORDER_REFUND",
                adminName,
                String.format("Order #%d refunded. Amount: %s. Restocked: %b", orderId, amount, restockItems)
        ));

        return savedOrder;
    }

    // Helpers for Dashboard
    public BigDecimal calculateTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .map(o -> o.getTotalAmount().subtract(o.getRefundedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getTotalOrderCount() { return orderRepository.count(); }
}