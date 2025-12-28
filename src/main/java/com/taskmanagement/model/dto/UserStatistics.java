package com.taskmanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserStatistics {
    private String userName;
    private long totalTasks;
    private long completedTasks;
    private long activeTasks;
    private double averageCompletionTimeHours;
    private double onTimePercentage;
}
