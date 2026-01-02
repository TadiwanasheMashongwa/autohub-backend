package com.autohub.api.controller;

import com.autohub.api.model.Part;
import com.autohub.api.service.PartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parts")
public class PartController {
    private final PartService service;

    public PartController(PartService service) {
        this.service = service;
    }

    @GetMapping
    public List<Part> getAllParts() {
        return service.getAllParts();
    }

    // NEW: The Global Search Endpoint
    @GetMapping("/search")
    public ResponseEntity<List<Part>> search(@RequestParam String query) {
        return ResponseEntity.ok(service.searchParts(query));
    }

    // NEW: Filter by Category Endpoint
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Part>> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(service.getPartsByCategory(categoryId));
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