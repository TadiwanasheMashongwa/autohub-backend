package com.autohub.api.repository;

import com.autohub.api.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // New: Support for the search bar
    @Query("SELECT c FROM Category c WHERE " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Category> searchCategories(@Param("query") String query);
    /**
     * Required for Catalog Management.
     * Ensures we don't create duplicate categories (e.g., 'Brakes' and 'BRAKES').
     */
    Optional<Category> findByName(String name);
}