package com.autohub.api.repository;

import com.autohub.api.model.Part;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {

    /**
     * Supports Endpoint #18 & #24: Barcode lookups for Warehouse Clerks.
     */
    Optional<Part> findByBarcode(String barcode);

    /**
     * Standard SKU lookup for internal inventory audits.
     */
    Optional<Part> findBySku(String sku);

    /**
     * AUDIT #3.2: Advanced Catalog Search.
     * Matches name, brand, SKU, or OEM numbers using case-insensitive partial matching.
     */
    @Query("SELECT p FROM Part p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.oemNumber) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Part> searchParts(@Param("query") String query, Pageable pageable);

    /**
     * Supports Endpoint #19: Browse by Category.
     */
    Page<Part> findByCategoryId(Long categoryId, Pageable pageable);

    /**
     * Supports Brand filtering for the frontend sidebar.
     */
    Page<Part> findByBrandIgnoreCase(String brand, Pageable pageable);

    /**
     * AUDIT #3.5 / Endpoint #26: Low Stock Reporting.
     * Identifies parts requiring immediate restock for the Admin Dashboard.
     */
    Page<Part> findByStockQuantityLessThan(int threshold, Pageable pageable);

    /**
     * AUDIT #5.1 / Endpoint #21: Vehicle Fitment/Compatibility lookup.
     * Ensures customers only see parts that fit their specific vehicle ID.
     */
    Page<Part> findByCompatibleVehiclesId(Long vehicleId, Pageable pageable);

    /**
     * PHASE 4 PERFORMANCE FIX:
     * Directly updates the average rating via Native SQL.
     * This bypasses Hibernate's dirty-checking and Versioning to avoid
     * OptimisticLockExceptions when multiple reviews arrive at once.
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE parts SET average_rating = :rating WHERE id = :partId", nativeQuery = true)
    void updateAverageRatingNative(@Param("partId") Long partId, @Param("rating") Double rating);
}