package com.autohub.api.service;

import com.autohub.api.model.Order;
import com.autohub.api.model.Transaction;
import com.autohub.api.repository.OrderRepository;
import com.autohub.api.repository.TransactionRepository; // Assuming this was created in Phase 1
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;

    public PaymentService(OrderRepository orderRepository, TransactionRepository transactionRepository) {
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Initiates a payment process.
     * In a real-world scenario, this would call the Paynow or Stripe API.
     */
    @Transactional
    public Map<String, String> initiatePayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Generate a mock reference (In production, this comes from the gateway)
        String gatewayRef = "AH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Create a record in our Transaction ledger
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
        response.put("paymentUrl", "https://checkout.autohub.co.zw/pay/" + gatewayRef); // Mock URL
        response.put("status", "PENDING");

        return response;
    }

    /**
     * Logic to verify payment status from the gateway.
     */
    @Transactional
    public void updateTransactionStatus(String gatewayRef, String status) {
        Transaction transaction = transactionRepository.findByGatewayReference(gatewayRef)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setStatus(status);
        transactionRepository.save(transaction);
    }
}