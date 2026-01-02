package com.autohub.api.service;

import com.autohub.api.model.Part;
import com.autohub.api.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public List<Part> searchParts(String query) {
        return partRepository.searchParts(query);
    }

    public List<Part> getPartsByCategory(Long categoryId) {
        return partRepository.findByCategoryId(categoryId);
    }

    public List<Part> getLowStockParts(int threshold) {
        return partRepository.findAll().stream()
                .filter(part -> part.getStockQuantity() <= threshold)
                .toList();
    }

    // NEW: Manual stock adjustment (Admin Override)
    @Transactional
    public Part updateStock(Long partId, Integer newQuantity) {
        Part part = getPartById(partId);
        if (newQuantity < 0) {
            throw new RuntimeException("Stock quantity cannot be negative");
        }
        part.setStockQuantity(newQuantity);
        return partRepository.save(part);
    }

    public Part savePart(Part part) {
        if (partRepository.findByBarcode(part.getBarcode()).isPresent()) {
            throw new RuntimeException("Duplicate Error: Barcode " + part.getBarcode() + " already exists.");
        }

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