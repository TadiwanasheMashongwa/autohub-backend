package com.autohub.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Category name is required")
    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @OneToMany(mappedBy = "category")
    @JsonIgnore // CRITICAL: Prevents Category -> Part -> Category loop in Swagger
    private List<Part> parts = new ArrayList<>();

    public Category() {}

    // --- GETTERS ---
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<Part> getParts() { return parts; }

    // --- SETTERS ---
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setParts(List<Part> parts) { this.parts = parts; }
}