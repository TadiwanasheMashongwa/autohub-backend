package com.autohub.api.controller;

import com.autohub.api.model.Order;
import com.autohub.api.model.User;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.service.OrderLifecycleService;
import com.autohub.api.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderLifecycleService lifecycleService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService,
                           OrderLifecycleService lifecycleService,
                           UserRepository userRepository) {
        this.orderService = orderService;
        this.lifecycleService = lifecycleService;
        this.userRepository = userRepository;
    }

    /**
     * Checkout cart → creates order + reserves inventory
     */
    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication
    ) {
        User user = getUser(authentication);
        return ResponseEntity.ok(
                orderService.checkoutCart(user, idempotencyKey)
        );
    }

    /**
     * FLOW 8.1 — Customer requests return (reason required)
     * Legal only from DELIVERED
     */
    @PostMapping("/{orderId}/return")
    public ResponseEntity<Order> requestReturn(
            @PathVariable Long orderId,
            @RequestParam String reason,
            Authentication authentication
    ) {
        // Ownership validation can be added later (Flow 10)
        return ResponseEntity.ok(
                lifecycleService.requestReturn(orderId, reason)
        );
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found with email: " + authentication.getName()));
    }
}
