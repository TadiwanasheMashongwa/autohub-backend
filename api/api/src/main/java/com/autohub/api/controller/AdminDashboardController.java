package com.autohub.api.controller;

import com.autohub.api.service.OrderService;
import com.autohub.api.service.PartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final OrderService orderService;
    private final PartService partService;

    public AdminDashboardController(OrderService orderService, PartService partService) {
        this.orderService = orderService;
        this.partService = partService;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", orderService.calculateTotalRevenue());
        stats.put("totalOrders", orderService.getTotalOrderCount());
        stats.put("lowStockCount", partService.getLowStockParts(5).size());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<?> getLowStockReport() {
        return ResponseEntity.ok(partService.getLowStockParts(5));
    }
}