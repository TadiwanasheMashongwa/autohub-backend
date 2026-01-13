package com.autohub.api.controller;

import com.autohub.api.model.Part;
import com.autohub.api.service.PartService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/parts")
public class PartController {
    private final PartService service;

    public PartController(PartService service) {
        this.service = service;
    }

    @GetMapping
    @Cacheable(value = "parts", key = "{#pageable.pageNumber, #pageable.pageSize}")
    public ResponseEntity<Page<Part>> getAllParts(Pageable pageable) {
        return ResponseEntity.ok(service.getAllParts(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Part>> search(@RequestParam String query, Pageable pageable) {
        return ResponseEntity.ok(service.searchParts(query, pageable));
    }

    /**
     * NEW: Quick-Scan endpoint for Warehouse staff.
     * Returns part details, bin location, and stock levels immediately.
     */
    @GetMapping("/scan/{barcode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK')")
    public ResponseEntity<Part> getPartByBarcode(@PathVariable String barcode) {
        return service.getPartByBarcode(barcode)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RuntimeException("No part found with barcode: " + barcode));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<Part>> getByCategory(@PathVariable Long categoryId, Pageable pageable) {
        return ResponseEntity.ok(service.getPartsByCategory(categoryId, pageable));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<Page<Part>> getByVehicle(@PathVariable Long vehicleId, Pageable pageable) {
        return ResponseEntity.ok(service.getPartsByVehicle(vehicleId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Part> addPart(@RequestBody Part part) {
        return ResponseEntity.ok(service.savePart(part));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Part> getPartById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPartById(id));
    }
}