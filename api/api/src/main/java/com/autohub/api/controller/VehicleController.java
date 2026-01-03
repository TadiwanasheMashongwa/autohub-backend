package com.autohub.api.controller;

import com.autohub.api.model.Vehicle;
import com.autohub.api.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<Vehicle> getAllVehicles() {
        return vehicleService.getAllVehicles();
    }

    @PostMapping
    public ResponseEntity<Vehicle> createVehicle(@Valid @RequestBody Vehicle vehicle) {
        return new ResponseEntity<>(vehicleService.saveVehicle(vehicle), HttpStatus.CREATED);
    }
}