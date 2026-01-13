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
@PreAuthorize("hasAnyRole('ADMIN', 'CLERK')") // Expanded access for warehouse clerks
public class AdminController {

    private final OrderService orderService;
    private final PartService partService;
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    public AdminController(OrderService orderService, PartService partService, UserRepository userRepository, AuthenticationService authenticationService) {
        this.orderService = orderService;
        this.partService = partService;
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
    }

    /**
     * NEW: Scan-to-Restock Endpoint.
     * Allows warehouse staff to increment stock by scanning a barcode.
     */
    @PatchMapping("/inventory/restock")
    public ResponseEntity<Part> restock(@RequestParam String barcode, @RequestParam Integer quantity) {
        return ResponseEntity.ok(partService.restockByBarcode(barcode, quantity));
    }

    // --- EXISTING DASHBOARD & CLERK METHODS ---
    @PostMapping("/create-clerk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createClerk(@RequestBody RegisterRequest request) {
        try {
            User clerk = authenticationService.createInternalUser(request, "ROLE_CLERK");
            return ResponseEntity.ok(Map.of("message", "Clerk created successfully", "email", clerk.getEmail()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", orderService.calculateTotalRevenue());
        stats.put("totalOrders", orderService.getTotalOrderCount());
        stats.put("totalCustomers", userRepository.countByRoleName("ROLE_CUSTOMER"));
        stats.put("lowStockCount", partService.getLowStockPartsList(5).size());
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/orders/{orderId}/ship")
    public ResponseEntity<Order> shipOrder(@PathVariable Long orderId, @RequestParam String courierName, @RequestParam String trackingNumber) {
        return ResponseEntity.ok(orderService.shipOrder(orderId, courierName, trackingNumber));
    }

    @PatchMapping("/inventory/{partId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Part> adjustStock(@PathVariable Long partId, @RequestParam Integer quantity) {
        return ResponseEntity.ok(partService.updateStock(partId, quantity));
    }

    @GetMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllCustomers() { return ResponseEntity.ok(userRepository.findAllCustomers()); }

    private User getUserFromAuth(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("Admin not found"));
    }
}