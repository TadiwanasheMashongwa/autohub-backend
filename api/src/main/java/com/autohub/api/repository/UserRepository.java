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

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String resetToken);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);

    @Query("SELECT u FROM User u WHERE u.role.name = 'ROLE_CLERK'")
    List<User> findAllClerks();

    /**
     * PHASE 3.2: Financial Intelligence Query.
     * Aggregates LTV, Order Counts, and Activity while maintaining identity fields (Email).
     */
    @Query("SELECT u.id as id, u.firstName as firstName, u.lastName as lastName, u.email as email, " +
            "u.businessName as businessName, u.phoneNumber as phoneNumber, u.address as address, " +
            "MAX(o.orderDate) as lastOrderDate, " +
            "COUNT(o.id) as orderCount, SUM(o.totalAmount) as totalSpent " +
            "FROM User u LEFT JOIN Order o ON u.id = o.user.id " +
            "WHERE u.role.name = 'ROLE_CUSTOMER' " +
            "GROUP BY u.id, u.firstName, u.lastName, u.email, u.businessName, u.phoneNumber, u.address")
    List<Map<String, Object>> findAllCustomersWithStats();

    @Query("SELECT u FROM User u WHERE " +
            "(LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "u.role.name = :roleName")
    List<User> searchByRole(@Param("query") String query, @Param("roleName") String roleName);
}