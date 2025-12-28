package com.taskmanagement.model.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "name", unique = true)
    public String name;

    @Column(name = "color")
    public String color;

    @OneToMany(mappedBy = "category")
    public List<Task> tasks = new ArrayList<>();
}
