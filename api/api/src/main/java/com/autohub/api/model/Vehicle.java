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
    private String engineCode; // e.g., 1KD-FTV (Very important for Zim market)
}