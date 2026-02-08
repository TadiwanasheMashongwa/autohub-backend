package com.autohub.api.controller;

import com.autohub.api.model.Part;
import com.autohub.api.service.PartService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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

    /* -------- PUBLIC OPERATIONS -------- */

    @GetMapping
    public ResponseEntity<Page<Part>> getAllParts(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String condition,
            Pageable pageable) {
        return ResponseEntity.ok(service.getAllParts(brand, condition, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Part>> search(
            @RequestParam String query,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String condition,
            Pageable pageable) {
        return ResponseEntity.ok(service.searchParts(query, brand, condition, pageable));
    }

    // 🛠️ NEW: Handles Category Filtering (e.g., /api/v1/parts/category/3)
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<Part>> getPartsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String condition,
            Pageable pageable) {
        return ResponseEntity.ok(service.getPartsByCategory(categoryId, brand, condition, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Part> getPartById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPartById(id));
    }

    /* -------- ADMIN OPERATIONS -------- */

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Part> createPart(@Valid @RequestBody Part part) {
        return new ResponseEntity<>(service.savePart(part), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Part> updatePart(@PathVariable Long id, @Valid @RequestBody Part part) {
        part.setId(id);
        return ResponseEntity.ok(service.savePart(part));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePart(@PathVariable Long id) {
        service.deletePart(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{partId}/compatibility/{vehicleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Part> addCompatibility(@PathVariable Long partId, @PathVariable Long vehicleId) {
        return ResponseEntity.ok(service.addVehicleCompatibility(partId, vehicleId));
    }

    @DeleteMapping("/{partId}/compatibility/{vehicleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Part> removeCompatibility(@PathVariable Long partId, @PathVariable Long vehicleId) {
        return ResponseEntity.ok(service.removeVehicleCompatibility(partId, vehicleId));
    }
}