package com.autohub.api.service;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderStatus;
import com.autohub.api.model.Transaction;
import com.autohub.api.repository.OrderRepository;
import com.autohub.api.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentOrchestratorService {

    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final InventoryService inventoryService;
    private final EmailService emailService;

    public PaymentOrchestratorService(
            OrderRepository orderRepository,
            TransactionRepository transactionRepository,
            InventoryService inventoryService,
            EmailService emailService
    ) {
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.inventoryService = inventoryService;
        this.emailService = emailService;
    }

    /**
     * STEP 5 — Idempotent payment finalization
     */
    @Transactional
    public Order finalizePayment(Long orderId, String gatewayReference) {

        Transaction tx = transactionRepository
                .findByGatewayReference(gatewayReference)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if ("SUCCESS".equals(tx.getStatus())) {
            return tx.getOrder(); // idempotent
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order not in payable state");
        }

        tx.setStatus("SUCCESS");
        transactionRepository.save(tx);

        inventoryService.deductReservedInventory(order);

        order.setStatus(OrderStatus.PAID);
        order.setPaymentId(gatewayReference);

        Order saved = orderRepository.save(order);

        emailService.sendOrderConfirmation(saved);

        return saved;
    }
}
