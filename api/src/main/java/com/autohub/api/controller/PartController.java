package com.autohub.api.controller;

import com.autohub.api.model.Part;
import com.autohub.api.service.PartService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/parts")
public class PartController {
    private final PartService service;

    public PartController(PartService service) {
        this.service = service;
    }

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

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<Part>> getByCategory(
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

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<Page<Part>> getByVehicle(@PathVariable Long vehicleId, Pageable pageable) {
        return ResponseEntity.ok(service.getPartsByVehicle(vehicleId, pageable));
    }
}