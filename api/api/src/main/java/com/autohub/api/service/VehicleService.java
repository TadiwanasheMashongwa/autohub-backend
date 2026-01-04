package com.autohub.api.service;

import com.autohub.api.model.Vehicle;
import com.autohub.api.repository.VehicleRepository;
import org.springframework.stereotype.Service;
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

    public Vehicle saveVehicle(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicle(Long id, Vehicle details) {
        Vehicle vehicle = getVehicleById(id);
        vehicle.setMake(details.getMake());
        vehicle.setModel(details.getModel());
        vehicle.setYearRange(details.getYearRange());
        vehicle.setEngineCode(details.getEngineCode());
        return vehicleRepository.save(vehicle);
    }

    public void deleteVehicle(Long id) {
        Vehicle vehicle = getVehicleById(id);
        vehicleRepository.delete(vehicle);
    }

    public List<String> getUniqueMakes() {
        return vehicleRepository.findDistinctMakes();
    }

    public List<String> getModelsByMake(String make) {
        return vehicleRepository.findDistinctModelsByMake(make);
    }
}