package com.autohub.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Category name is required")
    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    // MANUAL SETTERS
    public void setId(Long id) { this.id = id; }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // MANUAL GETTERS - Fixed to include description
    public Long getId() { return id; }

    public String getName() { return name; }

    public String getDescription() { return description; }
}