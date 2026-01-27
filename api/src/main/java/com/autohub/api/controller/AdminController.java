package com.autohub.api.controller;

import com.autohub.api.model.Order;
import com.autohub.api.repository.OrderRepository;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'CLERK')")
public class AdminController {

    private final OrderRepository orderRepository;
    private final OrderLifecycleService lifecycleService;
    private final UserRepository userRepository;
    private final PartService partService;
    private final OrderService orderService;

    public AdminController(OrderRepository orderRepository,
                           OrderLifecycleService lifecycleService,
                           UserRepository userRepository,
                           PartService partService,
                           OrderService orderService) {
        this.orderRepository = orderRepository;
        this.lifecycleService = lifecycleService;
        this.userRepository = userRepository;
        this.partService = partService;
        this.orderService = orderService;
    }

    @GetMapping("/orders/active")
    public ResponseEntity<List<Order>> getActiveOrders() {
        return ResponseEntity.ok(orderRepository.findAllActiveOrders());
    }

    @PatchMapping("/orders/{id}/logistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Order> updateLogistics(@PathVariable Long id, @RequestBody Map<String, String> logistics) {
        return ResponseEntity.ok(lifecycleService.overrideLogistics(
                id,
                logistics.get("courier"),
                logistics.get("tracking")
        ));
    }

    @PostMapping("/orders/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Order> processRefund(@PathVariable Long id) {
        return ResponseEntity.ok(lifecycleService.executeRefund(id));
    }

    @GetMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> customers() {
        return ResponseEntity.ok(userRepository.findAllCustomersWithStats());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of(
                "totalRevenue", orderService.calculateTotalRevenue(),
                "totalOrders", orderService.getTotalOrderCount(),
                "totalCustomers", userRepository.countByRoleName("ROLE_CUSTOMER"),
                "lowStockCount", partService.getLowStockParts().size()
        ));
    }
}