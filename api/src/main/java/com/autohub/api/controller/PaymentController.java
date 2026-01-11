package com.autohub.api.controller;

import com.autohub.api.model.Order;
import com.autohub.api.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final OrderService orderService;

    public PaymentController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/initiate/{orderId}")
    public ResponseEntity<Map<String, String>> initiatePayment(@PathVariable Long orderId) {
        // Logic to generate a payment intent (e.g., Paynow/Stripe integration)
        return ResponseEntity.ok(Map.of("status", "PENDING", "orderId", orderId.toString()));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Order> confirmPayment(@RequestBody Map<String, String> request) {
        Long orderId = Long.parseLong(request.get("orderId"));
        String paymentId = request.get("paymentId");
        return ResponseEntity.ok(orderService.confirmPayment(orderId, paymentId));
    }
}