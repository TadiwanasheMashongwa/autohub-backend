package com.autohub.api.repository;

import com.autohub.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Supports Security Stack: Standard username lookup.
     */
    Optional<User> findByUsername(String username);

    /**
     * CORE LOOKUP: Used by JWT authentication filter.
     * Since we use email for identity, this is the most-called method.
     */
    Optional<User> findByEmail(String email);

    /**
     * Supports Phase 6: Password Recovery.
     */
    Optional<User> findByResetToken(String resetToken);

    /**
     * AUDIT #11.2: Admin Dashboard Statistics.
     * Navigates the User -> Role relationship to provide real-time counts.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);

    /**
     * AUDIT #11.3: Customer Management.
     * Returns a list of all users specifically with the CUSTOMER role.
     */
    @Query("SELECT u FROM User u WHERE u.role.name = 'ROLE_CUSTOMER'")
    List<User> findAllCustomers();
    /**
     * PHASE 3: Staff Management.
     * Returns all users with the CLERK role.
     */
    @Query("SELECT u FROM User u WHERE u.role.name = 'ROLE_CLERK'")
    List<User> findAllClerks();
    @Query("SELECT u.id as id, u.firstName as firstName, u.lastName as lastName, u.email as email, " +
            "u.businessName as businessName, MAX(o.orderDate) as lastOrderDate, " +
            "COUNT(o.id) as orderCount, SUM(o.totalAmount) as totalSpent " +
            "FROM User u LEFT JOIN Order o ON u.id = o.user.id " +
            "WHERE u.role.name = 'ROLE_CUSTOMER' " +
            "GROUP BY u.id")
    List<Map<String, Object>> findAllCustomersWithStats();

    /**
     * PHASE 3: Global User Search.
     * Allows searching for staff by email or name.
     */
    @Query("SELECT u FROM User u WHERE " +
            "(LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "u.role.name = :roleName")
    List<User> searchByRole(@Param("query") String query, @Param("roleName") String roleName);
}