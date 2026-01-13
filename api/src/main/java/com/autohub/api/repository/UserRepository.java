package com.autohub.api.repository;

import com.autohub.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Core Lookups
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String resetToken);

    /**
     * FIX: Navigates the User -> Role relationship to count users by role name.
     * This is required for the Admin Dashboard stats.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);

    /**
     * FIX: Returns a list of all users with the CUSTOMER role.
     */
    @Query("SELECT u FROM User u WHERE u.role.name = 'ROLE_CUSTOMER'")
    List<User> findAllCustomers();
}