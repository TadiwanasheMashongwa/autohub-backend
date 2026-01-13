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

/**
 * COMMAND CENTER: Managed by Mike and his Warehouse Staff.
 * Synchronized with Master 53-endpoint list (v10.4.6).
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'CLERK')")
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

    // --- WAREHOUSE & LOGISTICS (Admin + Clerk) ---

    /**
     * ENDPOINT #44: Warehouse Picking Verification.
     * Ensures the correct part is in the box before shipping.
     */
    @PostMapping("/orders/{orderId}/pick")
    public ResponseEntity<Order> verifyAndPick(
            @PathVariable Long orderId,
            @RequestParam String barcode) {
        return ResponseEntity.ok(orderService.verifyAndPickItem(orderId, barcode));
    }

    /**
     * ENDPOINT #45: Logistics Processing.
     */
    @PostMapping("/orders/{orderId}/ship")
    public ResponseEntity<Order> shipOrder(
            @PathVariable Long orderId,
            @RequestParam String courierName,
            @RequestParam String trackingNumber) {
        return ResponseEntity.ok(orderService.shipOrder(orderId, courierName, trackingNumber));
    }

    /**
     * ENDPOINT #46: Transition order to IN_TRANSIT.
     */
    @PatchMapping("/orders/{orderId}/transit")
    public ResponseEntity<Order> setInTransit(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.transitOrder(orderId));
    }

    /**
     * ENDPOINT #50: Packing Slip Generation.
     */
    @GetMapping("/orders/{orderId}/manifest")
    public ResponseEntity<Map<String, Object>> getManifest(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderManifest(orderId));
    }

    /**
     * ENDPOINT #24: Quick-Scan restock for arriving shipments.
     */
    @PatchMapping("/inventory/restock")
    public ResponseEntity<Part> restock(@RequestParam String barcode, @RequestParam Integer quantity) {
        return ResponseEntity.ok(partService.updateStockByBarcode(barcode, quantity));
    }

    // --- ADMIN-ONLY OPERATIONS ---

    /**
     * ENDPOINT #8: Internal Staff Onboarding.
     */
    @PostMapping("/create-clerk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createClerk(@RequestBody RegisterRequest request) {
        try {
            User clerk = authenticationService.createInternalUser(request, "ROLE_CLERK");
            return ResponseEntity.ok(Map.of(
                    "message", "Clerk account created successfully",
                    "email", clerk.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ENDPOINT #51: Dashboard Financials & Alerts.
     */
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

    /**
     * ENDPOINT #25: Manual Inventory Override.
     */
    @PatchMapping("/inventory/{partId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Part> adjustStock(@PathVariable Long partId, @RequestParam Integer quantity) {
        return ResponseEntity.ok(partService.manualStockAdjustment(partId, quantity));
    }

    /**
     * ENDPOINT #11: CRM - Customer Management.
     */
    @GetMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllCustomers() {
        return ResponseEntity.ok(userRepository.findAllCustomers());
    }

    /**
     * ENDPOINT #49: Financial Reconciliation.
     */
    @PostMapping("/orders/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Order> issueRefund(
            @PathVariable Long orderId,
            @RequestParam BigDecimal amount,
            @RequestParam boolean restock) {
        return ResponseEntity.ok(orderService.processRefund(orderId, amount, restock));
    }

    private User getUserFromAuth(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Identity not found for: " + authentication.getName()));
    }
}