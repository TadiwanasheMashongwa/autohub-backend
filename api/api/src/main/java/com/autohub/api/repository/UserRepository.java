package com.autohub.api.repository;

import com.autohub.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    // NEW: Find user by token
    Optional<User> findByResetToken(String resetToken);

    @Query("SELECT u FROM User u WHERE u.role.name = 'ROLE_CUSTOMER'")
    List<User> findAllCustomers();

    long countByRoleName(String roleName);
}