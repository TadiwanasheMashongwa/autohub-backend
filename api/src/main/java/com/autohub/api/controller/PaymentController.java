package com.autohub.api.controller;

import com.autohub.api.model.Order;
import com.autohub.api.service.OrderService;
import com.autohub.api.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping("/initiate/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')") // SECURED
    public ResponseEntity<Map<String, String>> initiatePayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.initiatePayment(orderId));
    }

    /**
     * SECURE WEBHOOK: This endpoint is called directly by the Payment Gateway.
     * It uses a 'X-Gateway-Token' header to verify the source.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleGatewayCallback(
            @RequestHeader("X-Gateway-Token") String signature,
            @RequestBody Map<String, String> payload) {

        // 1. Authenticate the source
        if (!paymentService.isValidWebhookSignature(signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Signature");
        }

        // 2. Extract Data
        String paymentId = payload.get("paymentId");
        Long orderId = Long.parseLong(payload.get("orderId"));
        String status = payload.get("status");

        // 3. Process Success
        if ("PAID".equalsIgnoreCase(status)) {
            paymentService.updateTransactionStatus(paymentId, "SUCCESS");
            orderService.confirmPayment(orderId, paymentId);
            return ResponseEntity.ok("Webhook Processed Successfully");
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment Failed or Pending");
    }

    /**
     * Standard user-facing confirmation (should only be used for UI redirection).
     */
    @PostMapping("/confirm")
    @PreAuthorize("hasRole('CUSTOMER')") // SECURED
    public ResponseEntity<Order> confirmPayment(@RequestBody Map<String, String> request) {
        Long orderId = Long.parseLong(request.get("orderId"));
        String paymentId = request.get("paymentId");
        paymentService.updateTransactionStatus(paymentId, "SUCCESS");
        return ResponseEntity.ok(orderService.confirmPayment(orderId, paymentId));
    }
}