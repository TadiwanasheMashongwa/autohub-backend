package com.autohub.api.repository;

import com.autohub.api.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    @Query("SELECT DISTINCT v.make FROM Vehicle v")
    List<String> findDistinctMakes();

    @Query("SELECT DISTINCT v.model FROM Vehicle v WHERE v.make = :make")
    List<String> findDistinctModelsByMake(@Param("make") String make);

    @Query("SELECT DISTINCT v.yearRange FROM Vehicle v WHERE v.make = :make AND v.model = :model")
    List<String> findDistinctYearRangesByMakeAndModel(@Param("make") String make, @Param("model") String model);
}