package com.autohub.api.service;

import com.autohub.api.model.Part;
import com.autohub.api.model.Vehicle;
import com.autohub.api.repository.PartRepository;
import com.autohub.api.repository.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PartService {

    private final PartRepository partRepository;
    private final VehicleRepository vehicleRepository;
    private final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    public PartService(PartRepository partRepository, VehicleRepository vehicleRepository) {
        this.partRepository = partRepository;
        this.vehicleRepository = vehicleRepository;
    }

    // --- READ OPERATIONS ---

    public Page<Part> getAllParts(Pageable pageable) {
        return partRepository.findAll(pageable);
    }

    public Page<Part> searchParts(String query, Pageable pageable) {
        return partRepository.searchParts(query, pageable);
    }

    public Optional<Part> getPartByBarcode(String barcode) {
        return partRepository.findByBarcode(barcode);
    }

    public Page<Part> getPartsByCategory(Long categoryId, Pageable pageable) {
        return partRepository.findByCategoryId(categoryId, pageable);
    }

    public Page<Part> getPartsByVehicle(Long vehicleId, Pageable pageable) {
        return partRepository.findByCompatibleVehiclesId(vehicleId, pageable);
    }

    public Part getPartById(Long id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Part not found with ID: " + id));
    }

    /**
     * ENDPOINT #26: Retrieve all parts currently below the safety threshold.
     */
    public List<Part> getLowStockParts() {
        return partRepository.findByStockQuantityLessThan(DEFAULT_LOW_STOCK_THRESHOLD, Pageable.unpaged())
                .getContent();
    }

    // --- WRITE OPERATIONS ---

    @Transactional
    public Part savePart(Part part) {
        return partRepository.save(part);
    }

    /**
     * ENDPOINT #23: Admin SKU Deletion.
     */
    @Transactional
    public void deletePart(Long id) {
        if (!partRepository.existsById(id)) {
            throw new RuntimeException("Cannot delete. Part not found with ID: " + id);
        }
        partRepository.deleteById(id);
    }

    /**
     * ENDPOINT #24: Quick-Scan Restock logic for Warehouse Clerks.
     */
    @Transactional
    public Part updateStockByBarcode(String barcode, Integer quantity) {
        Part part = partRepository.findByBarcode(barcode)
                .orElseThrow(() -> new RuntimeException("Barcode not recognized: " + barcode));

        part.setStockQuantity(part.getStockQuantity() + quantity);
        return partRepository.save(part);
    }

    /**
     * ENDPOINT #25: Manual Stock Correction for Admins.
     */
    @Transactional
    public Part manualStockAdjustment(Long partId, Integer newQuantity) {
        Part part = getPartById(partId);
        part.setStockQuantity(newQuantity);
        return partRepository.save(part);
    }

    // --- FITMENT / COMPATIBILITY OPERATIONS ---

    /**
     * ENDPOINT #32: Link part to a specific vehicle model.
     */
    @Transactional
    public void addVehicleCompatibility(Long partId, Long vehicleId) {
        Part part = getPartById(partId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with ID: " + vehicleId));

        // Prevent duplicate compatibility entries
        if (!part.getCompatibleVehicles().contains(vehicle)) {
            part.getCompatibleVehicles().add(vehicle);
            partRepository.save(part);
        }
    }

    /**
     * ENDPOINT #33: Unlink a vehicle from a part.
     */
    @Transactional
    public void removeVehicleCompatibility(Long partId, Long vehicleId) {
        Part part = getPartById(partId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with ID: " + vehicleId));

        part.getCompatibleVehicles().remove(vehicle);
        partRepository.save(part);
    }
}