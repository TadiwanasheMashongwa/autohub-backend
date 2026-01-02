package com.autohub.api.service;

import com.autohub.api.model.Category;
import com.autohub.api.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category createCategory(Category category) {
        // Validation: Check if the category name already exists
        if (categoryRepository.findByName(category.getName()).isPresent()) {
            throw new RuntimeException("Duplicate Error: Category '" + category.getName() + "' already exists.");
        }

        return categoryRepository.save(category);
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }
}