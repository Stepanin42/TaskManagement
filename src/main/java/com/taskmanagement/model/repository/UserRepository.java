package com.taskmanagement.model.repository;

import com.taskmanagement.model.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    @Inject
    EntityManager em;

    public User findById(Long id) {
        return find("id", id).firstResult();
    }

    public List<User> findAllUser() {
        return find("").list();
    }

    @Transactional
    public User save(User user) {
        return em.merge(user);
    }
}
