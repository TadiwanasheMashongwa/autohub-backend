package com.autohub.api.service;

import com.autohub.api.model.AuditLog;
import com.autohub.api.model.Order;
import com.autohub.api.model.Transaction;
import com.autohub.api.repository.AuditLogRepository;
import com.autohub.api.repository.OrderRepository;
import com.autohub.api.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;

    @Value("${app.payment.webhook-secret:ah_default_secret}")
    private String webhookSecret;

    public PaymentService(OrderRepository orderRepository,
                          TransactionRepository transactionRepository,
                          AuditLogRepository auditLogRepository) {
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public boolean isValidWebhookSignature(String receivedSignature) {
        return webhookSecret.equals(receivedSignature);
    }

    @Transactional
    public Map<String, String> initiatePayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String gatewayRef = "AH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        transactionRepository.save(
                new Transaction(
                        order,
                        "AutoHub-Gateway",
                        gatewayRef,
                        order.getTotalAmount(),
                        "USD",
                        "PENDING"
                )
        );

        auditLogRepository.save(new AuditLog(
                "PAYMENT_INITIATED",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "Order #" + orderId + " Ref: " + gatewayRef
        ));

        Map<String, String> response = new HashMap<>();
        response.put("orderId", orderId.toString());
        response.put("gatewayReference", gatewayRef);
        response.put("status", "PENDING");
        return response;
    }
}
