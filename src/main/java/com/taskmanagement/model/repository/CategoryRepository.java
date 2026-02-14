package com.taskmanagement.model.repository;

import com.taskmanagement.model.entity.Category;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class  CategoryRepository implements PanacheRepository<Category> {

    public Category findByName(String name) {
        return find("name", name).firstResult();
    }
}
