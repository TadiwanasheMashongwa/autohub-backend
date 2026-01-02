package com.autohub.api.repository;

import com.autohub.api.model.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findByBarcode(String barcode);
    Optional<Part> findBySku(String sku);

    // NEW: Search across multiple fields (Case Insensitive)
    @Query("SELECT p FROM Part p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.oemNumber) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Part> searchParts(@Param("query") String query);

    // NEW: Filter by Category ID
    List<Part> findByCategoryId(Long categoryId);

    // NEW: Filter by Brand
    List<Part> findByBrandIgnoreCase(String brand);
}