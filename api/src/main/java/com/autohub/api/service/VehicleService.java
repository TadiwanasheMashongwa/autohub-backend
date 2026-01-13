package com.autohub.api.service;

import com.autohub.api.model.Vehicle;
import com.autohub.api.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
    }

    @Transactional
    public Vehicle saveVehicle(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle updateVehicle(Long id, Vehicle details) {
        Vehicle vehicle = getVehicleById(id);
        vehicle.setMake(details.getMake());
        vehicle.setModel(details.getModel());
        vehicle.setYearRange(details.getYearRange());
        vehicle.setEngineCode(details.getEngineCode());
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = getVehicleById(id);
        vehicleRepository.delete(vehicle);
    }

    /**
     * AUDIT #5.2 / Endpoint #27: Fitment Search - Step 1
     * Returns a unique list of manufacturers (e.g., Toyota, BMW, Ford).
     */
    public List<String> getUniqueMakes() {
        return vehicleRepository.findDistinctMakes();
    }

    /**
     * AUDIT #5.3 / Endpoint #28: Fitment Search - Step 2
     * Returns unique models based on a specific manufacturer (e.g., Corolla, Hilux).
     */
    public List<String> getModelsByMake(String make) {
        return vehicleRepository.findDistinctModelsByMake(make);
    }

    /**
     * AUDIT #5.4 / Endpoint #29: Fitment Search - Step 3
     * Returns the year ranges available for a specific model (e.g., 2012-2018).
     */
    public List<String> getYearRangesByMakeAndModel(String make, String model) {
        return vehicleRepository.findDistinctYearRangesByMakeAndModel(make, model);
    }
}