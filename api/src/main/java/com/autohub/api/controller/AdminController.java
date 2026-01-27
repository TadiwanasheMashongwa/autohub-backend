package com.autohub.api.controller;

import com.autohub.api.auth.AuthenticationService;
import com.autohub.api.auth.RegisterRequest;
import com.autohub.api.model.User;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'CLERK')")
public class AdminController {

    private final OrderService orderService;
    private final PartService partService;
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    public AdminController(OrderService orderService, PartService partService,
                           UserRepository userRepository, AuthenticationService authenticationService) {
        this.orderService = orderService;
        this.partService = partService;
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
    }

    @GetMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> customers() {
        return ResponseEntity.ok(userRepository.findAllCustomersWithStats());
    }

    @PostMapping("/create-clerk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createClerk(@RequestBody RegisterRequest request) {
        User clerk = authenticationService.createInternalUser(request, "ROLE_CLERK");
        return ResponseEntity.ok(Map.of("message", "Clerk initialized", "email", clerk.getEmail()));
    }

    @GetMapping("/clerks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllClerks() {
        return ResponseEntity.ok(userRepository.findAllClerks());
    }

    @DeleteMapping("/clerks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteClerk(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        if (user.getRole().getName().equals("ROLE_ADMIN")) throw new RuntimeException("Security: Cannot delete ROOT.");
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", orderService.calculateTotalRevenue());
        stats.put("totalOrders", orderService.getTotalOrderCount());
        stats.put("totalCustomers", userRepository.countByRoleName("ROLE_CUSTOMER"));
        stats.put("lowStockCount", partService.getLowStockParts().size());
        return ResponseEntity.ok(stats);
    }
}