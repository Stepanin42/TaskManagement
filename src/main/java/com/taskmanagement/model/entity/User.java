package com.taskmanagement.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.taskmanagement.model.enums.TaskStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "email", unique = true)
    public String email;

    @Column(name = "name")
    public String name;

    @OneToMany(mappedBy = "assignee", fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    public List<Task> tasks = new ArrayList<>();

    @Column(name = "active")
    public Boolean active = true;

    @CreationTimestamp
    @Column(name = "create_date")
    public LocalDateTime createdAt;

    @Transient // чтобы JPA не пытался маппить как колонку
    @JsonIgnore
    public int getActiveTasksCount() {
        return (int) tasks.stream()
                .filter(t -> t.status != TaskStatus.DONE && t.status != TaskStatus.CANCELLED)
                .count();
    }
}
