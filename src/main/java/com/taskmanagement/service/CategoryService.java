package com.taskmanagement.service;

import com.taskmanagement.model.entity.Category;
import com.taskmanagement.model.repository.CategoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
@ApplicationScoped
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Category> findAll() {
        return categoryRepository.findAll().list();
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category findByName(String name) {
        return categoryRepository.findByName(name);
    }

    @Transactional
    public void save(String name) {
        if (categoryRepository.findByName(name) != null) {
            return;
        }

        Category category = new Category();
        category.name = name;

        categoryRepository.persist(category);
    }
}
