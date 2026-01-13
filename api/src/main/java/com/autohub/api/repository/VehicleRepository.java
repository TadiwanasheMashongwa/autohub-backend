package com.autohub.api.repository;

import com.autohub.api.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    /**
     * Supports Phase 3: Fitment Search.
     * Returns all unique vehicle manufacturers (e.g., Toyota, Nissan).
     */
    @Query("SELECT DISTINCT v.make FROM Vehicle v")
    List<String> findDistinctMakes();

    /**
     * Supports Phase 3: Fitment Search.
     * Returns models based on a selected make (e.g., Hilux, Hardbody).
     */
    @Query("SELECT DISTINCT v.model FROM Vehicle v WHERE v.make = :make")
    List<String> findDistinctModelsByMake(@Param("make") String make);

    /**
     * Supports Phase 3: Fitment Search.
     * Returns year ranges for a specific make and model.
     */
    @Query("SELECT DISTINCT v.yearRange FROM Vehicle v WHERE v.make = :make AND v.model = :model")
    List<String> findDistinctYearRangesByMakeAndModel(@Param("make") String make, @Param("model") String model);
}