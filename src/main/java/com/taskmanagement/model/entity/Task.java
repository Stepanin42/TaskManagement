package com.taskmanagement.model.entity;

import com.taskmanagement.model.enums.TaskPriority;
import com.taskmanagement.model.enums.TaskStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "title")
    public String title;

    @Column(name = "description", length = 2000)
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    public TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    public TaskPriority priority = TaskPriority.MEDIUM;

    @ManyToOne
    @JoinColumn(name = "user_id")
    public User assignee;

    @ManyToOne
    @JoinColumn(name = "category_id")
    public Category category;

    @Column(name = "due_date")
    public LocalDate dueDate;

    @Column(name = "completed_date")
    public LocalDateTime completedAt;

    @Column(name = "estimated_hours")
    public Integer estimatedHours;

    @Column(name = "actual_hours")
    public Integer actualHours;

    @CreationTimestamp
    @Column(name = "create_date")
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "update_date")
    public LocalDateTime updatedAt;

    // Бизнес-методы
    public boolean isOverdue() {
        return dueDate != null
                && LocalDate.now().isAfter(dueDate)
                && status != TaskStatus.DONE
                && status != TaskStatus.CANCELLED;
    }

    public long getDaysUntilDue() {
        if (dueDate == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    public boolean isUrgent() {
        return priority == TaskPriority.URGENT || getDaysUntilDue() <= 1;
    }

    public void complete() {
        this.status = TaskStatus.DONE;
        this.completedAt = LocalDateTime.now();
    }

    public boolean canTransitionTo(TaskStatus newStatus) {
        switch (status) {
            case TODO:
                return newStatus == TaskStatus.IN_PROGRESS || newStatus == TaskStatus.CANCELLED;
            case IN_PROGRESS:
                return newStatus == TaskStatus.DONE || newStatus == TaskStatus.TODO || newStatus == TaskStatus.CANCELLED;
            case DONE:
                return newStatus == TaskStatus.TODO; // Reopen
            case CANCELLED:
                return newStatus == TaskStatus.TODO; // Reopen
            case OVERDUE:
                return newStatus == TaskStatus.IN_PROGRESS || newStatus == TaskStatus.DONE || newStatus == TaskStatus.CANCELLED;
            default:
                return false;
        }
    }
}
