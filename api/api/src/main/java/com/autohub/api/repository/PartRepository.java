package com.autohub.api.repository;

import com.autohub.api.model.Part;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findByBarcode(String barcode);
    Optional<Part> findBySku(String sku);

    @Query("SELECT p FROM Part p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.oemNumber) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Part> searchParts(@Param("query") String query, Pageable pageable);

    Page<Part> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Part> findByBrandIgnoreCase(String brand, Pageable pageable);
    Page<Part> findByStockQuantityLessThan(int threshold, Pageable pageable);
    Page<Part> findByCompatibleVehiclesId(Long vehicleId, Pageable pageable);

    /**
     * UPDATED: Direct DB update to bypass Optimistic Locking (@Version) issues.
     */
    @Modifying
    @Query("UPDATE Part p SET p.averageRating = :rating WHERE p.id = :partId")
    void updateAverageRating(@Param("partId") Long partId, @Param("rating") Double rating);
}