package com.autohub.api.controller;

import com.autohub.api.model.Vehicle;
import com.autohub.api.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    // --- SHARED / PUBLIC ENDPOINTS ---

    /**
     * Retrieves all vehicles for general browsing.
     */
    @GetMapping
    public List<Vehicle> getAllVehicles() {
        return vehicleService.getAllVehicles();
    }

    /**
     * ENDPOINT #30: Detailed view of a specific vehicle model.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    /**
     * ENDPOINT #27: Drill-down Start - Get all available manufacturers.
     */
    @GetMapping("/makes")
    public List<String> getMakes() {
        return vehicleService.getUniqueMakes();
    }

    /**
     * ENDPOINT #28: Drill-down Step 2 - Filter models by the selected make.
     */
    @GetMapping("/models")
    public List<String> getModels(@RequestParam String make) {
        return vehicleService.getModelsByMake(make);
    }

    /**
     * ENDPOINT #29: Drill-down Step 3 - Filter years by make and model.
     */
    @GetMapping("/years")
    public List<String> getYears(@RequestParam String make, @RequestParam String model) {
        return vehicleService.getYearRangesByMakeAndModel(make, model);
    }

    // --- ADMIN PROTECTED ENDPOINTS ---

    /**
     * ENDPOINT #31: Add a new vehicle to the database.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Vehicle> createVehicle(@Valid @RequestBody Vehicle vehicle) {
        return new ResponseEntity<>(vehicleService.saveVehicle(vehicle), HttpStatus.CREATED);
    }

    /**
     * Admin: Update vehicle metadata (Fixing typos in years or models).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Vehicle> updateVehicle(@PathVariable Long id, @Valid @RequestBody Vehicle vehicle) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, vehicle));
    }

    /**
     * Admin: Remove a vehicle from the system.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}