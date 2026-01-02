package com.autohub.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank; // Add this import
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

    @NotBlank(message = "Category name is required") // Validation added
    @Column(nullable = false, unique = true)
    private String name;
    public void setName(String name){
        this.name=name;
    };

    private String description;
}