package com.autohub.api.controller;

import com.autohub.api.model.Order;
import com.autohub.api.model.Role;
import com.autohub.api.model.User;
import com.autohub.api.repository.OrderRepository;
import com.autohub.api.repository.RoleRepository;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public AdminController(OrderRepository orderRepository,
                           OrderLifecycleService lifecycleService,
                           UserRepository userRepository,
                           PartService partService,
                           OrderService orderService,
                           PasswordEncoder passwordEncoder,
                           RoleRepository roleRepository) {
        this.orderRepository = orderRepository;
        this.lifecycleService = lifecycleService;
        this.userRepository = userRepository;
        this.partService = partService;
        this.orderService = orderService;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }

    // --- ORDER & DASHBOARD TERMINAL ---

    @GetMapping("/orders/active")
    public ResponseEntity<List<Order>> getActiveOrders() {
        return ResponseEntity.ok(orderRepository.findAllActiveOrders());
    }

    @PostMapping("/orders/{id}/verify-pick")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK')")
    public ResponseEntity<Order> verifyPick(@PathVariable Long id, @RequestBody Map<String, String> verifyMap) {
        return ResponseEntity.ok(lifecycleService.verifyAndPick(id, verifyMap));
    }

    @PatchMapping("/orders/{id}/logistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Order> updateLogistics(@PathVariable Long id, @RequestBody Map<String, String> logistics) {
        return ResponseEntity.ok(lifecycleService.overrideLogistics(id, logistics.get("courier"), logistics.get("tracking")));
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

    // --- CLERK MANAGEMENT (Role Mapping Fixed) ---

    @GetMapping("/clerks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getClerks() {
        return ResponseEntity.ok(userRepository.findAllClerks());
    }

    @GetMapping("/clerks/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> searchClerks(@RequestParam String query) {
        return ResponseEntity.ok(userRepository.searchByRole(query, "ROLE_CLERK"));
    }

    @PostMapping("/create-clerk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createClerk(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Conflict: Email already registered in system."));
        }

        User clerk = new User();
        clerk.setFirstName(request.get("firstName"));
        clerk.setLastName(request.get("lastName"));
        clerk.setEmail(email);
        clerk.setUsername(email); // Align with UserDetails getUsername mapping
        clerk.setPassword(passwordEncoder.encode(request.get("password")));

        // Fetch ROLE_CLERK from DB
        Role clerkRole = roleRepository.findByName("ROLE_CLERK")
                .orElseThrow(() -> new RuntimeException("Deployment Error: ROLE_CLERK not found in database."));

        // FIXED: Using single setRole() to match your User entity
        clerk.setRole(clerkRole);

        userRepository.save(clerk);

        return ResponseEntity.ok(Map.of("message", "Clerk initialized successfully."));
    }

    @DeleteMapping("/clerks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteClerk(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Operator access revoked."));
    }
}