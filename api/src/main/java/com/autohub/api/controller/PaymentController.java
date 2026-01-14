package com.autohub.api.controller;

import com.autohub.api.model.Order;
import com.autohub.api.service.OrderLifecycleService;
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
    private final OrderLifecycleService lifecycle;

    public PaymentController(PaymentService paymentService,
                             OrderLifecycleService lifecycle) {
        this.paymentService = paymentService;
        this.lifecycle = lifecycle;
    }

    @PostMapping("/initiate/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, String>> initiate(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.initiatePayment(orderId));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestHeader("X-Gateway-Token") String signature,
            @RequestBody Map<String, String> payload) {

        if (!paymentService.isValidWebhookSignature(signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Signature");
        }

        lifecycle.markPaid(
                Long.parseLong(payload.get("orderId")),
                payload.get("paymentId")
        );

        return ResponseEntity.ok("Webhook processed");
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Order> confirm(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(
                lifecycle.markPaid(
                        Long.parseLong(request.get("orderId")),
                        request.get("paymentId")
                )
        );
    }
}
