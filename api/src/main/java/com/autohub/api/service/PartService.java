package com.autohub.api.service;

import com.autohub.api.model.Part;
import com.autohub.api.repository.PartRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PartService {

    private final PartRepository partRepository;
    private final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    public PartService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    // --- FRONTEND FILTERING OPERATIONS ---

    public Page<Part> getAllParts(String brand, String condition, Pageable pageable) {
        return partRepository.findAllWithFilters(brand, condition, pageable);
    }

    public Page<Part> searchParts(String query, String brand, String condition, Pageable pageable) {
        return partRepository.searchWithFilters(query, brand, condition, pageable);
    }

    public Page<Part> getPartsByCategory(Long categoryId, String brand, String condition, Pageable pageable) {
        return partRepository.findByCategoryWithFilters(categoryId, brand, condition, pageable);
    }

    public Page<Part> getPartsByVehicle(Long vehicleId, Pageable pageable) {
        return partRepository.findByCompatibleVehiclesId(vehicleId, pageable);
    }

    public Part getPartById(Long id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Part not found with ID: " + id));
    }

    // --- ADMIN & INVENTORY OPERATIONS (Restored) ---

    /**
     * Required by AdminController.stats()
     */
    public List<Part> getLowStockParts() {
        // Fetches parts below threshold for the dashboard
        return partRepository.findByStockQuantityLessThan(DEFAULT_LOW_STOCK_THRESHOLD, Pageable.unpaged())
                .getContent();
    }

    /**
     * Required by AdminController.restock()
     */
    @Transactional
    public Part updateStockByBarcode(String barcode, Integer quantity) {
        Part part = partRepository.findByBarcode(barcode)
                .orElseThrow(() -> new RuntimeException("Barcode not recognized: " + barcode));

        part.setStockQuantity(part.getStockQuantity() + quantity);
        return partRepository.save(part);
    }

    /**
     * Required by AdminController.adjustStock()
     */
    @Transactional
    public Part manualStockAdjustment(Long partId, Integer newQuantity) {
        Part part = getPartById(partId);
        part.setStockQuantity(newQuantity);
        return partRepository.save(part);
    }

    // --- HELPER WRAPPERS ---

    public Optional<Part> getPartByBarcode(String barcode) {
        return partRepository.findByBarcode(barcode);
    }

    @Transactional
    public Part savePart(Part part) {
        return partRepository.save(part);
    }

    @Transactional
    public void deletePart(Long id) {
        if (!partRepository.existsById(id)) {
            throw new RuntimeException("Cannot delete. Part not found with ID: " + id);
        }
        partRepository.deleteById(id);
    }
}