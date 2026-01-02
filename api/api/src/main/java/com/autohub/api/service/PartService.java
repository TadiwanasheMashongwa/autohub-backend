package com.autohub.api.service;

import com.autohub.api.model.Part;
import com.autohub.api.repository.PartRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PartService {

    private final PartRepository partRepository;

    public PartService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    public List<Part> getAllParts() {
        return partRepository.findAll();
    }

    public List<Part> getLowStockParts(int threshold) {
        // FIXED: Uses 'partRepository' to match the constructor variable name
        return partRepository.findAll().stream()
                .filter(part -> part.getStockQuantity() <= threshold)
                .toList();
    }

    public Part savePart(Part part) {
        // 1. Strict Barcode Validation
        if (partRepository.findByBarcode(part.getBarcode()).isPresent()) {
            throw new RuntimeException("Duplicate Error: Barcode " + part.getBarcode() + " already exists.");
        }

        // 2. Strict SKU Validation
        if (partRepository.findBySku(part.getSku()).isPresent()) {
            throw new RuntimeException("Duplicate Error: SKU " + part.getSku() + " already exists.");
        }

        return partRepository.save(part);
    }

    public Optional<Part> getPartByBarcode(String barcode) {
        return partRepository.findByBarcode(barcode);
    }

    public Part getPartById(Long id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Part not found with id: " + id));
    }
}