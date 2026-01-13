package com.autohub.api.repository;

import com.autohub.api.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Required for Catalog Management.
     * Ensures we don't create duplicate categories (e.g., 'Brakes' and 'BRAKES').
     */
    Optional<Category> findByName(String name);
}