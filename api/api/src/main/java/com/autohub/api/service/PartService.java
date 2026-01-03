package com.autohub.api.service;

import com.autohub.api.model.AuditLog;
import com.autohub.api.model.Part;
import com.autohub.api.repository.AuditLogRepository;
import com.autohub.api.repository.PartRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PartService {

    private final PartRepository partRepository;
    private final AuditLogRepository auditLogRepository;

    public PartService(PartRepository partRepository, AuditLogRepository auditLogRepository) {
        this.partRepository = partRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public Page<Part> getAllParts(Pageable pageable) {
        return partRepository.findAll(pageable);
    }

    public Page<Part> searchParts(String query, Pageable pageable) {
        return partRepository.searchParts(query, pageable);
    }

    public Page<Part> getPartsByCategory(Long categoryId, Pageable pageable) {
        return partRepository.findByCategoryId(categoryId, pageable);
    }

    // Paginated version for full reports
    public Page<Part> getLowStockParts(int threshold, Pageable pageable) {
        return partRepository.findByStockQuantityLessThan(threshold, pageable);
    }

    // NEW Helper: Non-paginated list for quick dashboard stats
    public List<Part> getLowStockPartsList(int threshold) {
        return partRepository.findAll().stream()
                .filter(p -> p.getStockQuantity() < threshold)
                .collect(Collectors.toList());
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