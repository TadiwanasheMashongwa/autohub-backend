package com.autohub.api.repository;

import com.autohub.api.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * CORE LOOKUP: Used by AuthenticationService.
     * Maps string role names (e.g., "ROLE_CLERK") to their database entities.
     * Essential for Phase 6 Access Control and Admin Onboarding.
     */
    Optional<Role> findByName(String name);
}