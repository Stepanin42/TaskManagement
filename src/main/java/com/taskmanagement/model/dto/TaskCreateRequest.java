package com.taskmanagement.model.dto;

import com.taskmanagement.model.enums.TaskPriority;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskCreateRequest {
    private String title;
    private String description;
    private TaskPriority priority;
    private LocalDate dueDate;
    private Long assigneeId;
    private Long categoryId;
    private Integer estimatedHours;
}
