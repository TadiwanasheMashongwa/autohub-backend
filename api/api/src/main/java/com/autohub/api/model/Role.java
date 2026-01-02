package com.autohub.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    // Manual No-Args Constructor
    public Role() {}

    // Manual All-Args Constructor
    public Role(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // --- MANUAL GETTERS (Fixes the AuthenticationService error) ---
    public Long getId() { return id; }
    public String getName() { return name; }

    // --- MANUAL SETTERS ---
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
}