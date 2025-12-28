package com.taskmanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TaskStatistics {
    private long total;
    private long todo;
    private long inProgress;
    private long done;
    private long cancelled;
    private long overdue;

    public double getCompletionRate() {
        return total > 0 ? (done * 100.0 / total) : 0.0;
    }
}
