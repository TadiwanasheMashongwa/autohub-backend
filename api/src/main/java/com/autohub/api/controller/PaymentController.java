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

    /**
     * Phase 6: Handshake Initiation
     * Returns the Stripe clientSecret to the React frontend.
     */
    @PostMapping("/initiate/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, String>> initiate(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.initiatePayment(orderId));
    }

    /**
     * Phase 6: Final Confirmation
     * Called by the frontend AFTER Stripe has successfully charged the card.
     */
    @PostMapping("/confirm")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Order> confirm(@RequestBody Map<String, String> request) {
        // We use 'paymentIntentId' to match the payload from your frontend CheckoutForm
        return ResponseEntity.ok(
                lifecycle.markPaid(
                        Long.parseLong(request.get("orderId")),
                        request.get("paymentIntentId")
                )
        );
    }

    /**
     * Webhook for asynchronous server-to-server updates from Stripe.
     * Note: In production, Stripe uses a special 'Stripe-Signature' header.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody String payload) {

        // This is a placeholder for your Webhook implementation
        // For local development, we'll keep it simple:
        return ResponseEntity.ok("Received");
    }
}