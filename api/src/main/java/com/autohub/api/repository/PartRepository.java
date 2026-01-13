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

    Optional<Part> findByBarcode(String barcode);

    Optional<Part> findBySku(String sku);

    /**
     * Supports Advanced Search: Matches name, brand, SKU, or OEM numbers.
     */
    @Query("SELECT p FROM Part p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.oemNumber) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Part> searchParts(@Param("query") String query, Pageable pageable);

    /**
     * Supports Category browsing.
     */
    Page<Part> findByCategoryId(Long categoryId, Pageable pageable);

    /**
     * Supports Brand filtering.
     */
    Page<Part> findByBrandIgnoreCase(String brand, Pageable pageable);

    /**
     * PHASE 3: Warehouse & Admin Low Stock Reporting.
     */
    Page<Part> findByStockQuantityLessThan(int threshold, Pageable pageable);

    /**
     * PHASE 3: Vehicle Fitment/Compatibility lookup.
     */
    Page<Part> findByCompatibleVehiclesId(Long vehicleId, Pageable pageable);

    /**
     * NATIVE FIX: Direct SQL update to force the change into the database
     * without Hibernate's version/dirty checking interference.
     * Essential for the Review/Rating sync logic.
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE parts SET average_rating = :rating WHERE id = :partId", nativeQuery = true)
    void updateAverageRatingNative(@Param("partId") Long partId, @Param("rating") Double rating);
}