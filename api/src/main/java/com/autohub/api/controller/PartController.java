package com.autohub.api.controller;

import com.autohub.api.model.Part;
import com.autohub.api.service.PartService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parts")
public class PartController {
    private final PartService service;

    public PartController(PartService service) {
        this.service = service;
    }

    // --- PUBLIC ENDPOINTS ---

    @GetMapping
    @Cacheable(value = "parts", key = "{#pageable.pageNumber, #pageable.pageSize}")
    public ResponseEntity<Page<Part>> getAllParts(Pageable pageable) {
        return ResponseEntity.ok(service.getAllParts(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Part>> search(@RequestParam String query, Pageable pageable) {
        return ResponseEntity.ok(service.searchParts(query, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Part> getPartById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPartById(id));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<Part>> getByCategory(@PathVariable Long categoryId, Pageable pageable) {
        return ResponseEntity.ok(service.getPartsByCategory(categoryId, pageable));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<Page<Part>> getByVehicle(@PathVariable Long vehicleId, Pageable pageable) {
        return ResponseEntity.ok(service.getPartsByVehicle(vehicleId, pageable));
    }

    // --- WAREHOUSE & CLERK ENDPOINTS ---

    /**
     * ENDPOINT #18: Quick-Scan lookup.
     */
    @GetMapping("/scan/{barcode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK')")
    public ResponseEntity<Part> getPartByBarcode(@PathVariable String barcode) {
        return service.getPartByBarcode(barcode)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RuntimeException("No part found with barcode: " + barcode));
    }

    /**
     * ENDPOINT #24: Quick Restock via Barcode Scan.
     */
    @PatchMapping("/admin/inventory/restock")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK')")
    public ResponseEntity<Part> quickRestock(@RequestParam String barcode, @RequestParam Integer quantity) {
        return ResponseEntity.ok(service.updateStockByBarcode(barcode, quantity));
    }

    // --- ADMIN INVENTORY CONTROL ---

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Part> addPart(@RequestBody Part part) {
        return ResponseEntity.ok(service.savePart(part));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePart(@PathVariable Long id) {
        service.deletePart(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * ENDPOINT #25: Manual Stock Adjustment.
     */
    @PatchMapping("/admin/inventory/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Part> adjustStock(@PathVariable Long id, @RequestParam Integer newQuantity) {
        return ResponseEntity.ok(service.manualStockAdjustment(id, newQuantity));
    }

    /**
     * ENDPOINT #26: Low Stock Alerts for Admin Dashboard.
     */
    @GetMapping("/admin/inventory/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Part>> getLowStockParts() {
        return ResponseEntity.ok(service.getLowStockParts());
    }

    // --- FITMENT & COMPATIBILITY ---

    @PostMapping("/{partId}/compatibility/{vehicleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addVehicleCompatibility(@PathVariable Long partId, @PathVariable Long vehicleId) {
        service.addVehicleCompatibility(partId, vehicleId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{partId}/compatibility/{vehicleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeVehicleCompatibility(@PathVariable Long partId, @PathVariable Long vehicleId) {
        service.removeVehicleCompatibility(partId, vehicleId);
        return ResponseEntity.ok().build();
    }
}