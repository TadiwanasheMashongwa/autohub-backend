package com.autohub.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter @Setter
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
}