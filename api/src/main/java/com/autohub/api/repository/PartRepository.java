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

    // --- LOGIC: UNIFIED FILTERING (Case-Insensitive Restoration) ---

    @Query("SELECT p FROM Part p WHERE " +
            "(:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) AND " +
            "(:condition IS NULL OR LOWER(p.condition) = LOWER(:condition))")
    Page<Part> findAllWithFilters(@Param("brand") String brand,
                                  @Param("condition") String condition,
                                  Pageable pageable);

    @Query("SELECT p FROM Part p WHERE " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.oemNumber) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) AND " +
            "(:condition IS NULL OR LOWER(p.condition) = LOWER(:condition))")
    Page<Part> searchWithFilters(@Param("query") String query,
                                 @Param("brand") String brand,
                                 @Param("condition") String condition,
                                 Pageable pageable);

    @Query("SELECT p FROM Part p WHERE p.category.id = :categoryId AND " +
            "(:brand IS NULL OR LOWER(p.brand) = LOWER(:brand)) AND " +
            "(:condition IS NULL OR LOWER(p.condition) = LOWER(:condition))")
    Page<Part> findByCategoryWithFilters(@Param("categoryId") Long categoryId,
                                         @Param("brand") String brand,
                                         @Param("condition") String condition,
                                         Pageable pageable);

    // --- LOGIC: LOGISTICS & INVENTORY ---

    Page<Part> findByCompatibleVehiclesId(Long vehicleId, Pageable pageable);

    Optional<Part> findByBarcode(String barcode);

    Page<Part> findByStockQuantityLessThan(int threshold, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "UPDATE parts SET average_rating = :rating WHERE id = :partId", nativeQuery = true)
    void updateAverageRatingNative(@Param("partId") Long partId, @Param("rating") double rating);
}