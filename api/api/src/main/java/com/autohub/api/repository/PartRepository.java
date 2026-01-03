package com.autohub.api.repository;

import com.autohub.api.model.Part;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findByBarcode(String barcode);
    Optional<Part> findBySku(String sku);

    // UPDATED: Search with Pagination support
    @Query("SELECT p FROM Part p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.oemNumber) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Part> searchParts(@Param("query") String query, Pageable pageable);

    // UPDATED: Filter by Category ID with Pagination
    Page<Part> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Part> findByBrandIgnoreCase(String brand, Pageable pageable);
}