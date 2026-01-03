package com.autohub.api.service;

import com.autohub.api.model.AuditLog;
import com.autohub.api.model.Part;
import com.autohub.api.repository.AuditLogRepository;
import com.autohub.api.repository.PartRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class PartService {

    private final PartRepository partRepository;
    private final AuditLogRepository auditLogRepository; // NEW

    public PartService(PartRepository partRepository, AuditLogRepository auditLogRepository) {
        this.partRepository = partRepository;
        this.auditLogRepository = auditLogRepository;
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

    @Transactional
    public Part updateStock(Long partId, Integer newQuantity) {
        Part part = getPartById(partId);
        int oldQuantity = part.getStockQuantity();

        if (newQuantity < 0) {
            throw new RuntimeException("Stock quantity cannot be negative");
        }

        part.setStockQuantity(newQuantity);
        Part updatedPart = partRepository.save(part);

        // NEW: RECORD THE AUDIT LOG
        String adminName = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogRepository.save(new AuditLog(
                "STOCK_ADJUSTMENT",
                adminName,
                String.format("Part: %s, SKU: %s, Adjusted from %d to %d", part.getName(), part.getSku(), oldQuantity, newQuantity)
        ));

        return updatedPart;
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