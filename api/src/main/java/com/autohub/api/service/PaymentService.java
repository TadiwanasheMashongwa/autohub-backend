package com.autohub.api.service;

import com.autohub.api.model.AuditLog;
import com.autohub.api.model.Order;
import com.autohub.api.model.Transaction;
import com.autohub.api.repository.AuditLogRepository;
import com.autohub.api.repository.OrderRepository;
import com.autohub.api.repository.TransactionRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${app.payment.webhook-secret}")
    private String webhookSecret;

    public PaymentService(
            OrderRepository orderRepository,
            TransactionRepository transactionRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Security check for Stripe Webhooks
     */
    public boolean isValidWebhookSignature(String receivedSignature) {
        return webhookSecret.equals(receivedSignature);
    }

    /**
     * Phase 6: The Transactional Handshake
     * Communicates with Stripe API to create a PaymentIntent.
     */
    @Transactional
    public Map<String, String> initiatePayment(Long orderId) {
        // 1. Authenticate the SDK with your sk_test key from application.properties
        Stripe.apiKey = stripeApiKey;

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        try {
            // 2. Stripe Unit Conversion (USD 10.50 -> 1050 cents)
            long amountInCents = order.getTotalAmount()
                    .multiply(new BigDecimal(100))
                    .longValue();

            // 3. Configure Stripe Intent Parameters
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putMetadata("order_id", orderId.toString())
                    .putMetadata("customer_email", SecurityContextHolder.getContext().getAuthentication().getName())
                    .build();

            // 4. Fire the request to Stripe's Servers
            PaymentIntent intent = PaymentIntent.create(params);

            // 5. Atomic Log: Save Transaction record in 'PENDING' state
            transactionRepository.save(new Transaction(
                    order,
                    "Stripe-Gateway",
                    intent.getId(), // This is our source of truth for the payment
                    order.getTotalAmount(),
                    "USD",
                    "PENDING"
            ));

            // 6. Audit Trail
            auditLogRepository.save(new AuditLog(
                    "PAYMENT_INTENT_CREATED",
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    "Initialized payment for Order #" + orderId + " (Stripe ID: " + intent.getId() + ")"
            ));

            // 7. Payload for Frontend Elements
            Map<String, String> response = new HashMap<>();
            response.put("orderId", orderId.toString());
            response.put("clientSecret", intent.getClientSecret());
            response.put("status", "PENDING");

            return response;

        } catch (Exception e) {
            // Fallback: Log the failure to the console for debugging
            System.err.println("CRITICAL: Stripe Handshake Failed -> " + e.getMessage());
            throw new RuntimeException("Stripe Gateway Communication Error: " + e.getMessage());
        }
    }
}