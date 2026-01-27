package com.autohub.api.service;

import com.autohub.api.model.Category;
import com.autohub.api.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }
    public List<Category> searchCategories(String query) {
        return categoryRepository.searchCategories(query);
    }

    /**
     * AUDIT #8.2: Logic to create a new category with duplicate check.
     */
    @Transactional
    public Category createCategory(Category category) {
        if (categoryRepository.findByName(category.getName()).isPresent()) {
            throw new RuntimeException("Duplicate Error: Category '" + category.getName() + "' already exists.");
        }
        return categoryRepository.save(category);
    }

    /**
     * AUDIT #8.4: Logic to update metadata while maintaining name uniqueness.
     */
    @Transactional
    public Category updateCategory(Long id, Category details) {
        Category category = getCategoryById(id);

        // Prevent duplicate names on update if name changed
        categoryRepository.findByName(details.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new RuntimeException("Category name already exists.");
                    }
                });

        category.setName(details.getName());
        category.setDescription(details.getDescription());
        return categoryRepository.save(category);
    }

    /**
     * AUDIT #8.5: Delete category logic.
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }
}