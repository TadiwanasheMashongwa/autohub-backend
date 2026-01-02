package com.autohub.api.repository;

import com.autohub.api.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Required for the unique name validation in CategoryService
    Optional<Category> findByName(String name);
}