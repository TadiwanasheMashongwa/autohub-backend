package com.autohub.api.controller;

import com.autohub.api.auth.AuthenticationService;
import com.autohub.api.auth.RegisterRequest;
import com.autohub.api.model.Order;
import com.autohub.api.model.Part;
import com.autohub.api.model.User;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.service.*;
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
@PreAuthorize("hasAnyRole('ADMIN', 'CLERK')")
public class AdminController {

    private final OrderService orderService;
    private final PartService partService;
    private final WarehouseService warehouseService;
    private final LogisticsService logisticsService;
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    public AdminController(OrderService orderService,
                           PartService partService,
                           WarehouseService warehouseService,
                           LogisticsService logisticsService,
                           UserRepository userRepository,
                           AuthenticationService authenticationService) {
        this.orderService = orderService;
        this.partService = partService;
        this.warehouseService = warehouseService;
        this.logisticsService = logisticsService;
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
    }

    // -------- WAREHOUSE --------

    @PostMapping("/orders/{orderId}/pick")
    public ResponseEntity<Order> verifyAndPick(
            @PathVariable Long orderId,
            @RequestParam String barcode) {
        return ResponseEntity.ok(
                warehouseService.verifyAndPickItem(orderId, barcode)
        );
    }

    // -------- LOGISTICS --------

    @PostMapping("/orders/{orderId}/ship")
    public ResponseEntity<Order> shipOrder(
            @PathVariable Long orderId,
            @RequestParam String courierName,
            @RequestParam String trackingNumber) {
        return ResponseEntity.ok(
                logisticsService.shipOrder(orderId, courierName, trackingNumber)
        );
    }

    @PatchMapping("/orders/{orderId}/transit")
    public ResponseEntity<Order> setInTransit(@PathVariable Long orderId) {
        return ResponseEntity.ok(
                logisticsService.markInTransit(orderId)
        );
    }

    // -------- INVENTORY --------

    @PatchMapping("/inventory/restock")
    public ResponseEntity<Part> restock(@RequestParam String barcode,
                                        @RequestParam Integer quantity) {
        return ResponseEntity.ok(
                partService.updateStockByBarcode(barcode, quantity)
        );
    }

    // -------- ADMIN --------

    @PostMapping("/create-clerk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createClerk(@RequestBody RegisterRequest request) {
        User clerk = authenticationService.createInternalUser(request, "ROLE_CLERK");
        return ResponseEntity.ok(Map.of(
                "message", "Clerk account created successfully",
                "email", clerk.getEmail()
        ));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", orderService.calculateTotalRevenue());
        stats.put("totalOrders", orderService.getTotalOrderCount());
        stats.put("totalCustomers", userRepository.countByRoleName("ROLE_CUSTOMER"));
        stats.put("lowStockCount", partService.getLowStockParts().size());
        return ResponseEntity.ok(stats);
    }

    @PatchMapping("/inventory/{partId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Part> adjustStock(@PathVariable Long partId,
                                            @RequestParam Integer quantity) {
        return ResponseEntity.ok(
                partService.manualStockAdjustment(partId, quantity)
        );
    }

    @GetMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllCustomers() {
        return ResponseEntity.ok(userRepository.findAllCustomers());
    }

    @PostMapping("/orders/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Order> issueRefund(
            @PathVariable Long orderId,
            @RequestParam BigDecimal amount,
            @RequestParam boolean restock) {
        return ResponseEntity.ok(
                orderService.processRefund(orderId, amount, restock)
        );
    }

    private User getUserFromAuth(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Identity not found for: " + authentication.getName()));
    }
}
