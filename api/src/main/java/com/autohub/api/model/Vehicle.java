package com.autohub.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String make;  // e.g., Toyota
    private String model; // e.g., Hilux
    private String yearRange; // e.g., 2015-2022
    private String engineCode; // e.g., 1KD-FTV

    // MANUAL GETTERS - Ensuring Postman/Jackson visibility
    public Long getId() { return id; }
    public String getMake() { return make; }
    public String getModel() { return model; }
    public String getYearRange() { return yearRange; }
    public String getEngineCode() { return engineCode; }

    // MANUAL SETTERS
    public void setId(Long id) { this.id = id; }
    public void setMake(String make) { this.make = make; }
    public void setModel(String model) { this.model = model; }
    public void setYearRange(String yearRange) { this.yearRange = yearRange; }
    public void setEngineCode(String engineCode) { this.engineCode = engineCode; }
}