package com.autohub.api.controller;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderStatus;
import com.autohub.api.model.User;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    // 🛠️ NEW: Endpoint for Clerk to see the Warehouse Queue (PAID, PICKED orders)
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK')")
    public ResponseEntity<List<Order>> getActiveOrders() {
        return ResponseEntity.ok(orderService.getAllActiveOrders());
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Order> checkout(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User identity lost during checkout"));

        Order order = orderService.checkoutCart(user, idempotencyKey);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<Order>> getMyOrders() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(orderService.findOrdersByUser(user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'CLERK')")
    public ResponseEntity<Order> getOrderDetails(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PatchMapping("/{id}/logistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK')")
    public ResponseEntity<Order> updateLogistics(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload
    ) {
        String courier = payload.get("courier");
        String tracking = payload.get("tracking");
        return ResponseEntity.ok(orderService.updateLogistics(id, courier, tracking));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK')")
    public ResponseEntity<Order> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload
    ) {
        OrderStatus status = OrderStatus.valueOf(payload.get("status"));
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }
}