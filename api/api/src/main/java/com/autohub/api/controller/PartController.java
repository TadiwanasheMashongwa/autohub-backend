package com.autohub.api.controller;

import com.autohub.api.model.Part;
import com.autohub.api.service.PartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/parts")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @GetMapping
    public List<Part> getAllParts() {
        return partService.getAllParts();
    }

    @PostMapping
    public ResponseEntity<Part> createPart(@Valid @RequestBody Part part) {
        return new ResponseEntity<>(partService.savePart(part), HttpStatus.CREATED);
    }

    // ADD THIS: The Scanner Search Endpoint
    @GetMapping("/search")
    public ResponseEntity<Part> getPartByBarcode(@RequestParam String barcode) {
        return partService.getPartByBarcode(barcode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}