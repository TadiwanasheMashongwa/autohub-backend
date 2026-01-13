package com.autohub.api.controller;

import com.autohub.api.model.Order;
import com.autohub.api.service.OrderService;
import com.autohub.api.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    public PaymentController(PaymentService paymentService, OrderService orderService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
    }

    /**
     * Endpoint to start the payment process for an order.
     */
    @PostMapping("/initiate/{orderId}")
    public ResponseEntity<Map<String, String>> initiatePayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.initiatePayment(orderId));
    }

    /**
     * Mock Confirmation endpoint.
     * In production, this would be a secure Webhook or a Redirect URL.
     */
    @PostMapping("/confirm")
    public ResponseEntity<Order> confirmPayment(@RequestBody Map<String, String> request) {
        Long orderId = Long.parseLong(request.get("orderId"));
        String paymentId = request.get("paymentId"); // The reference from the gateway

        // 1. Update the ledger
        paymentService.updateTransactionStatus(paymentId, "SUCCESS");

        // 2. Finalize the order (Stock deduction & status update)
        return ResponseEntity.ok(orderService.confirmPayment(orderId, paymentId));
    }
}