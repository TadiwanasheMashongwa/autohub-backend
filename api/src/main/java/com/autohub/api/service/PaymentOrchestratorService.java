package com.autohub.api.service;

import com.autohub.api.model.*;
import com.autohub.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentOrchestratorService {

    private final OrderRepository orderRepository;
    private final PartRepository partRepository;
    private final TransactionRepository transactionRepository;
    private final EmailService emailService;

    public PaymentOrchestratorService(
            OrderRepository orderRepository,
            PartRepository partRepository,
            TransactionRepository transactionRepository,
            EmailService emailService
    ) {
        this.orderRepository = orderRepository;
        this.partRepository = partRepository;
        this.transactionRepository = transactionRepository;
        this.emailService = emailService;
    }

    /**
     * STEP 4 — SINGLE AUTHORITATIVE PAYMENT FINALIZATION
     *
     * Invariants:
     * - Idempotent
     * - Inventory deducted once
     * - Order transitions once
     */
    @Transactional
    public Order finalizePayment(Long orderId, String gatewayReference) {

        Transaction transaction = transactionRepository
                .findByGatewayReference(gatewayReference)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + gatewayReference));

        // 🔒 Idempotency guard
        if ("SUCCESS".equals(transaction.getStatus())) {
            return transaction.getOrder();
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // 🔐 State guard
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException(
                    "Order is not eligible for payment finalization. Current state: " + order.getStatus()
            );
        }

        // 1️⃣ Mark transaction successful
        transaction.setStatus("SUCCESS");
        transactionRepository.save(transaction);

        // 2️⃣ Deduct inventory ONCE
        for (OrderItem item : order.getItems()) {
            Part part = item.getPart();

            if (part.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for SKU: " + part.getSku());
            }

            part.setStockQuantity(part.getStockQuantity() - item.getQuantity());
            partRepository.save(part);
        }

        // 3️⃣ Transition order state
        order.setPaymentId(gatewayReference);
        order.setPaymentStatus("PAID");
        order.setStatus(OrderStatus.PAID);

        Order savedOrder = orderRepository.save(order);

        // 4️⃣ Side effect (exactly once)
        emailService.sendOrderConfirmation(savedOrder);

        return savedOrder;
    }
}
