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
     * AUDIT #5.2 / Endpoint #27: Fitment Search - Step 1.
     * Fetches a clean list of manufacturers to start the search funnel.
     */
    @Query("SELECT DISTINCT v.make FROM Vehicle v ORDER BY v.make ASC")
    List<String> findDistinctMakes();

    /**
     * AUDIT #5.3 / Endpoint #28: Fitment Search - Step 2.
     * Fetches models belonging to a specific manufacturer.
     */
    @Query("SELECT DISTINCT v.model FROM Vehicle v WHERE v.make = :make ORDER BY v.model ASC")
    List<String> findDistinctModelsByMake(@Param("make") String make);

    /**
     * AUDIT #5.4 / Endpoint #29: Fitment Search - Step 3.
     * Pinpoints available year ranges/variants for the selected vehicle.
     */
    @Query("SELECT DISTINCT v.yearRange FROM Vehicle v WHERE v.make = :make AND v.model = :model ORDER BY v.yearRange ASC")
    List<String> findDistinctYearRangesByMakeAndModel(@Param("make") String make, @Param("model") String model);
}