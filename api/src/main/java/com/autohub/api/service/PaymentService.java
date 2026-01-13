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

    // FIXED: Added ":ah_default_secret" as a fallback to prevent app crash if property is missing
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

    /**
     * PHASE 4: Initiate Payment Lifecycle.
     */
    @Transactional
    public Map<String, String> initiatePayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String gatewayRef = "AH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transaction transaction = new Transaction(
                order,
                "AutoHub-Gateway",
                gatewayRef,
                order.getTotalAmount(),
                "USD",
                "PENDING"
        );
        transactionRepository.save(transaction);

        // Audit Log Sync
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogRepository.save(new AuditLog(
                "PAYMENT_INITIATED",
                currentUser,
                "User initiated payment for Order #" + orderId + " Ref: " + gatewayRef
        ));

        Map<String, String> response = new HashMap<>();
        response.put("orderId", orderId.toString());
        response.put("gatewayReference", gatewayRef);
        response.put("status", "PENDING");
        return response;
    }

    /**
     * PHASE 4: Update transaction status for audit and gateway sync.
     */
    @Transactional
    public void updateTransactionStatus(String gatewayRef, String status) {
        Transaction transaction = transactionRepository.findByGatewayReference(gatewayRef)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        String oldStatus = transaction.getStatus();
        transaction.setStatus(status);
        transactionRepository.save(transaction);

        auditLogRepository.save(new AuditLog(
                "TRANSACTION_STATUS_CHANGE",
                "SYSTEM_GATEWAY",
                "Transaction " + gatewayRef + " changed from " + oldStatus + " to " + status
        ));
    }
}