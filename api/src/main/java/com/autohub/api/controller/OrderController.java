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
    public ResponseEntity<Order> checkout(
            @RequestParam Long vehicleId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication
    ) {
        User user = getUserFromAuth(authentication);
        return ResponseEntity.ok(orderService.checkoutCart(user, vehicleId, idempotencyKey));
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

    @PostMapping("/{id}/confirm-receipt")
    public ResponseEntity<Order> confirmReceipt(@PathVariable Long id, Authentication authentication) {
        Order order = orderService.getOrderByIdSecurely(id, authentication.getName());

        if (order.getStatus() != OrderStatus.SHIPPED &&
                order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new RuntimeException("Order must be SHIPPED or IN_TRANSIT before confirmation.");
        }

        return ResponseEntity.ok(orderService.updateStatus(id, OrderStatus.COMPLETED));
    }

    private User getUserFromAuth(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + authentication.getName()));
    }
}
