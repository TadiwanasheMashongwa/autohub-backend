package com.autohub.api.repository;

import com.autohub.api.model.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long> {
    // This method allows the API to find a part instantly when you scan a barcode
    Optional<Part> findByBarcode(String barcode);
    Optional<Part> findBySku(String sku);
}