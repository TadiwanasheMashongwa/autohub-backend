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

    public Part savePart(Part part) {
        // Check Barcode
        if (partRepository.findByBarcode(part.getBarcode()).isPresent()) {
            throw new RuntimeException("Duplicate Error: Barcode " + part.getBarcode() + " already exists.");
        }

        // Check SKU (You'll need findBySku in your Repository)
        if (partRepository.findBySku(part.getSku()).isPresent()) {
            throw new RuntimeException("Duplicate Error: SKU " + part.getSku() + " already exists.");
        }

        return partRepository.save(part);
    }

    // ADD THIS: Logic to find the part by barcode
    public Optional<Part> getPartByBarcode(String barcode) {
        return partRepository.findByBarcode(barcode);
    }
}