package com.autohub.api.controller;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderStatus;
import com.autohub.api.model.User;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, Authentication authentication) {
        User user = getUserFromAuth(authentication);
        return ResponseEntity.ok(orderService.checkoutCart(user, idempotencyKey));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrders(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        return ResponseEntity.ok(orderService.getOrdersByUser(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(orderService.getOrderByIdSecurely(id, authentication.getName()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // --- CUSTOMER ACTION: CONFIRM RECEIPT ---
    @PostMapping("/{id}/confirm-receipt")
    public ResponseEntity<Order> confirmReceipt(@PathVariable Long id, Authentication authentication) {
        // 1. Verify ownership and existence
        Order order = orderService.getOrderByIdSecurely(id, authentication.getName());

        // 2. Business Logic: Only shipped orders can be confirmed
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new RuntimeException("Order must be SHIPPED before you can confirm receipt.");
        }

        // 3. Automate the completion
        return ResponseEntity.ok(orderService.updateStatus(id, OrderStatus.COMPLETED));
    }

    // --- ADMIN ACTION: MANUAL OVERRIDE ---
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }

    private User getUserFromAuth(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}