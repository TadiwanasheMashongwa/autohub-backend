package com.autohub.api.controller;

import com.autohub.api.model.Order;
import com.autohub.api.model.Part;
import com.autohub.api.model.User;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.service.OrderService;
import com.autohub.api.service.PartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final OrderService orderService;
    private final PartService partService;
    private final UserRepository userRepository;

    public AdminDashboardController(OrderService orderService, PartService partService, UserRepository userRepository) {
        this.orderService = orderService;
        this.partService = partService;
        this.userRepository = userRepository;
    }
    @PostMapping("/orders/{orderId}/refund")
    public ResponseEntity<Order> issueRefund(
            @PathVariable Long orderId,
            @RequestParam BigDecimal amount,
            @RequestParam boolean restock) {
        return ResponseEntity.ok(orderService.processRefund(orderId, amount, restock));
    }
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", orderService.calculateTotalRevenue());
        stats.put("totalOrders", orderService.getTotalOrderCount());
        stats.put("totalCustomers", userRepository.countByRoleName("ROLE_CUSTOMER"));
        stats.put("lowStockCount", partService.getLowStockParts(5).size());
        return ResponseEntity.ok(stats);
    }

    // NEW: Customer Management Endpoint
    @GetMapping("/customers")
    public ResponseEntity<List<User>> getAllCustomers() {
        return ResponseEntity.ok(userRepository.findAllCustomers());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Part>> getLowStockReport() {
        return ResponseEntity.ok(partService.getLowStockParts(5));
    }

    // NEW: Manual Inventory Adjustment Endpoint
    @PatchMapping("/inventory/{partId}/stock")
    public ResponseEntity<Part> adjustStock(@PathVariable Long partId, @RequestParam Integer quantity) {
        return ResponseEntity.ok(partService.updateStock(partId, quantity));
    }
}