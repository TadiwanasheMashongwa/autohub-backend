package com.autohub.api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @GetMapping("/stats")
    public String getDashboardStats() {
        return "Total Sales: $5000 | New Orders: 12";
    }
}