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

    public PartService(PartRepository partRepository, VehicleRepository vehicleRepository) {
        this.partRepository = partRepository;
        this.vehicleRepository = vehicleRepository;
    }

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

    @Transactional
    public Part savePart(Part part) {
        return partRepository.save(part);
    }

    /**
     * AUDIT #11.6: Delete Part.
     * Resolves the red error in PartController.
     */
    @Transactional
    public void deletePart(Long id) {
        if (!partRepository.existsById(id)) {
            throw new RuntimeException("Cannot delete. Part not found with ID: " + id);
        }
        partRepository.deleteById(id);
    }

    /**
     * PHASE 3: Warehouse & Fitment Logic.
     * Maps a part to a vehicle to resolve red error in PartController.
     */
    @Transactional
    public void addVehicleCompatibility(Long partId, Long vehicleId) {
        Part part = getPartById(partId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        part.getCompatibleVehicles().add(vehicle);
        partRepository.save(part);
    }

    /**
     * PHASE 3: Remove Fitment Mapping.
     */
    @Transactional
    public void removeVehicleCompatibility(Long partId, Long vehicleId) {
        Part part = getPartById(partId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        part.getCompatibleVehicles().remove(vehicle);
        partRepository.save(part);
    }

    /**
     * PHASE 3: Scan-to-Restock Logic.
     */
    @Transactional
    public Part restockByBarcode(String barcode, Integer quantity) {
        Part part = partRepository.findByBarcode(barcode)
                .orElseThrow(() -> new RuntimeException("Barcode not recognized: " + barcode));
        part.setStockQuantity(part.getStockQuantity() + quantity);
        return partRepository.save(part);
    }

    @Transactional
    public Part updateStock(Long partId, Integer quantity) {
        Part part = getPartById(partId);
        part.setStockQuantity(quantity);
        return partRepository.save(part);
    }

    /**
     * PHASE 3: Low Stock Reporting.
     */
    public List<Part> getLowStockPartsList(int threshold) {
        return partRepository.findByStockQuantityLessThan(threshold, Pageable.unpaged()).getContent();
    }
}