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

    @Query("SELECT p FROM Part p WHERE " +
            "(:brand IS NULL OR LOWER(CAST(p.brand AS string)) = LOWER(CAST(:brand AS string))) AND " +
            "(:condition IS NULL OR LOWER(CAST(p.condition AS string)) = LOWER(CAST(:condition AS string)))")
    Page<Part> findAllWithFilters(@Param("brand") String brand,
                                  @Param("condition") String condition,
                                  Pageable pageable);

    @Query("SELECT p FROM Part p WHERE " +
            "(LOWER(CAST(p.name AS string)) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(CAST(p.sku AS string)) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(CAST(p.oemNumber AS string)) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:brand IS NULL OR LOWER(CAST(p.brand AS string)) = LOWER(CAST(:brand AS string))) AND " +
            "(:condition IS NULL OR LOWER(CAST(p.condition AS string)) = LOWER(CAST(:condition AS string)))")
    Page<Part> searchWithFilters(@Param("query") String query,
                                 @Param("brand") String brand,
                                 @Param("condition") String condition,
                                 Pageable pageable);

    @Query("SELECT p FROM Part p WHERE p.category.id = :categoryId AND " +
            "(:brand IS NULL OR LOWER(CAST(p.brand AS string)) = LOWER(CAST(:brand AS string))) AND " +
            "(:condition IS NULL OR LOWER(CAST(p.condition AS string)) = LOWER(CAST(:condition AS string)))")
    Page<Part> findByCategoryWithFilters(@Param("categoryId") Long categoryId,
                                         @Param("brand") String brand,
                                         @Param("condition") String condition,
                                         Pageable pageable);

    Page<Part> findByCompatibleVehiclesId(Long vehicleId, Pageable pageable);

    Optional<Part> findByBarcode(String barcode);

    Page<Part> findByStockQuantityLessThan(int threshold, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "UPDATE parts SET average_rating = :rating WHERE id = :partId", nativeQuery = true)
    void updateAverageRatingNative(@Param("partId") Long partId, @Param("rating") double rating);
}