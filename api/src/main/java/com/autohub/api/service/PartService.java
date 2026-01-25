package com.autohub.api.service;

import com.autohub.api.model.Part;
import com.autohub.api.repository.PartRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PartService {
    private final PartRepository partRepository;

    public PartService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

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
        return partRepository.findById(id).orElseThrow(() -> new RuntimeException("Part not found"));
    }
}