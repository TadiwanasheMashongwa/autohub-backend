package com.autohub.api.repository;

import com.autohub.api.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    // This interface is empty because JpaRepository provides
    // all the standard CRUD (Create, Read, Update, Delete) methods for us.
}