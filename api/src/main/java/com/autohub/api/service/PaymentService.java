package com.autohub.api.service;

import com.autohub.api.model.Order;
import com.autohub.api.model.Transaction;
import com.autohub.api.repository.OrderRepository;
import com.autohub.api.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;

    @Value("${app.payment.webhook-secret}")
    private String webhookSecret; // Configured in application.properties

    public PaymentService(OrderRepository orderRepository, TransactionRepository transactionRepository) {
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Security Check: Validates that the POST request to our webhook
     * contains the correct secret key or hash signature.
     */
    public boolean isValidWebhookSignature(String receivedSignature) {
        // In a real Paynow/Stripe integration, you'd perform a HMAC-SHA256 check here.
        return webhookSecret.equals(receivedSignature);
    }

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

        Map<String, String> response = new HashMap<>();
        response.put("orderId", orderId.toString());
        response.put("gatewayReference", gatewayRef);
        response.put("status", "PENDING");
        return response;
    }

    @Transactional
    public void updateTransactionStatus(String gatewayRef, String status) {
        Transaction transaction = transactionRepository.findByGatewayReference(gatewayRef)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setStatus(status);
        transactionRepository.save(transaction);
    }
}