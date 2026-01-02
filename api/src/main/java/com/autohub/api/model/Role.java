package com.autohub.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // e.g., "ROLE_USER", "ROLE_ADMIN"

    // MANUAL GETTER - This fixes the "cannot find symbol getName()" error
    public String getName() {
        return this.name;
    }
}