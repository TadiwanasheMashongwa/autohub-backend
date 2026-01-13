package com.autohub.api.controller;

import com.autohub.api.auth.AuthenticationService;
import com.autohub.api.auth.RegisterRequest;
import com.autohub.api.model.Order;
import com.autohub.api.model.Part;
import com.autohub.api.model.User;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.service.OrderService;
import com.autohub.api.service.PartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OrderService orderService;
    private final PartService partService;
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    public AdminController(OrderService orderService,
                           PartService partService,
                           UserRepository userRepository,
                           AuthenticationService authenticationService) {
        this.orderService = orderService;
        this.partService = partService;
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
    }

    /**
     * Allows the Admin to create a Clerk account.
     */
    @PostMapping("/create-clerk")
    public ResponseEntity<?> createClerk(@RequestBody RegisterRequest request) {
        try {
            User clerk = authenticationService.createInternalUser(request, "ROLE_CLERK");
            return ResponseEntity.ok(Map.of(
                    "message", "Clerk created successfully",
                    "email", clerk.getEmail(),
                    "displayName", clerk.getActualUsername()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --- DASHBOARD & STATS ---
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", orderService.calculateTotalRevenue());
        stats.put("totalOrders", orderService.getTotalOrderCount());
        // Note: countByRoleName will be fixed in Step 3
        stats.put("totalCustomers", userRepository.countByRoleName("ROLE_CUSTOMER"));
        stats.put("lowStockCount", partService.getLowStockPartsList(5).size());
        return ResponseEntity.ok(stats);
    }

    // --- ORDER FULFILLMENT ---
    @PostMapping("/orders/{orderId}/ship")
    public ResponseEntity<Order> shipOrder(
            @PathVariable Long orderId,
            @RequestParam String courierName,
            @RequestParam String trackingNumber) {
        return ResponseEntity.ok(orderService.shipOrder(orderId, courierName, trackingNumber));
    }

    @PostMapping("/orders/{orderId}/refund")
    public ResponseEntity<Order> issueRefund(
            @PathVariable Long orderId,
            @RequestParam BigDecimal amount,
            @RequestParam boolean restock) {
        return ResponseEntity.ok(orderService.processRefund(orderId, amount, restock));
    }

    // --- INVENTORY & CUSTOMERS ---
    @GetMapping("/low-stock")
    public ResponseEntity<List<Part>> getLowStockReport() {
        return ResponseEntity.ok(partService.getLowStockPartsList(5));
    }

    @PatchMapping("/inventory/{partId}/stock")
    public ResponseEntity<Part> adjustStock(@PathVariable Long partId, @RequestParam Integer quantity) {
        return ResponseEntity.ok(partService.updateStock(partId, quantity));
    }

    @GetMapping("/customers")
    public ResponseEntity<List<User>> getAllCustomers() {
        return ResponseEntity.ok(userRepository.findAllCustomers());
    }

    /**
     * Helper to get User by Email from the Authentication context.
     */
    private User getUserFromAuth(Authentication authentication) {
        // FIX: Standardized to findByEmail
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Admin not found with email: " + authentication.getName()));
    }
}