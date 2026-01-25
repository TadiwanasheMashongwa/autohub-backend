package com.autohub.api.repository;

import com.autohub.api.model.Part;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {

    @Query("SELECT p FROM Part p WHERE " +
            "(:brand IS NULL OR p.brand = :brand) AND " +
            "(:condition IS NULL OR p.condition = :condition)")
    Page<Part> findAllWithFilters(@Param("brand") String brand,
                                  @Param("condition") String condition,
                                  Pageable pageable);

    @Query("SELECT p FROM Part p WHERE " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.oemNumber) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:brand IS NULL OR p.brand = :brand) AND " +
            "(:condition IS NULL OR p.condition = :condition)")
    Page<Part> searchWithFilters(@Param("query") String query,
                                 @Param("brand") String brand,
                                 @Param("condition") String condition,
                                 Pageable pageable);

    @Query("SELECT p FROM Part p WHERE p.category.id = :categoryId AND " +
            "(:brand IS NULL OR p.brand = :brand) AND " +
            "(:condition IS NULL OR p.condition = :condition)")
    Page<Part> findByCategoryWithFilters(@Param("categoryId") Long categoryId,
                                         @Param("brand") String brand,
                                         @Param("condition") String condition,
                                         Pageable pageable);

    Page<Part> findByCompatibleVehiclesId(Long vehicleId, Pageable pageable);
}