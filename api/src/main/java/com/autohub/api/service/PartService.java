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

@Service
public class PartService {

    private final PartRepository partRepository;
    private final VehicleRepository vehicleRepository;
    private final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    public PartService(PartRepository partRepository, VehicleRepository vehicleRepository) {
        this.partRepository = partRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public Page<Part> getAllParts(String brand, String condition, Pageable pageable) {
        return partRepository.findAllWithFilters(brand, condition, pageable);
    }

    public Page<Part> searchParts(String query, String brand, String condition, Pageable pageable) {
        return partRepository.searchWithFilters(query, brand, condition, pageable);
    }

    public Part getPartById(Long id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Part not found with ID: " + id));
    }

    @Transactional
    public Part savePart(Part part) {
        return partRepository.save(part);
    }

    @Transactional
    public void deletePart(Long id) {
        partRepository.deleteById(id);
    }

    @Transactional
    public Part addVehicleCompatibility(Long partId, Long vehicleId) {
        Part part = getPartById(partId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (!part.getCompatibleVehicles().contains(vehicle)) {
            part.getCompatibleVehicles().add(vehicle);
        }
        return partRepository.save(part);
    }

    @Transactional
    public Part removeVehicleCompatibility(Long partId, Long vehicleId) {
        Part part = getPartById(partId);
        part.getCompatibleVehicles().removeIf(v -> v.getId().equals(vehicleId));
        return partRepository.save(part);
    }

    public List<Part> getLowStockParts() {
        return partRepository.findByStockQuantityLessThan(DEFAULT_LOW_STOCK_THRESHOLD, Pageable.unpaged()).getContent();
    }

    @Transactional
    public Part updateStockByBarcode(String barcode, Integer quantity) {
        Part part = partRepository.findByBarcode(barcode)
                .orElseThrow(() -> new RuntimeException("Barcode not recognized"));
        part.setStockQuantity(part.getStockQuantity() + quantity);
        return partRepository.save(part);
    }

    @Transactional
    public Part manualStockAdjustment(Long partId, Integer newQuantity) {
        Part part = getPartById(partId);
        part.setStockQuantity(newQuantity);
        return partRepository.save(part);
    }
}