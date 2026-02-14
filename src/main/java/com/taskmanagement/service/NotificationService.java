package com.taskmanagement.service;

import com.taskmanagement.model.entity.Task;
import com.taskmanagement.model.entity.User;
import com.taskmanagement.model.enums.TaskStatus;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class NotificationService {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);
    //Логи тут для имитации


    /**
     * Уведомление о назначении задачи
     */
    public void notifyTaskAssigned(Task task) {
        if (task.assignee == null) return;

        String message = String.format(
                "New task assigned to you: '%s' (Priority: %s, Due: %s)",
                task.title,
                task.priority,
                task.dueDate != null ? task.dueDate : "No deadline"
        );

        LOG.info("Notification to {}: {}", task.assignee.email, message);
    }

    /**
     * Уведомление об изменении статуса
     */
    public void notifyStatusChanged(Task task, TaskStatus oldStatus) {
        if (task.assignee == null) return;

        String message = String.format(
                "Task '%s' status changed: %s -> %s",
                task.title,
                oldStatus,
                task.status
        );

        LOG.info("Notification to {}: {}", task.assignee.email, message);
    }

    /**
     * Уведомление о снятии назначения
     */
    public void notifyTaskUnassigned(Task task, User previousAssignee) {
        String message = String.format(
                "Task '%s' has been reassigned to %s",
                task.title,
                task.assignee.name
        );

        LOG.info("Notification to {}: {}", previousAssignee.email, message);
    }

    /**
     * Напоминание о приближающемся дедлайне
     */
    public void notifyUpcomingDeadline(Task task) {
        if (task.assignee == null) return;

        long daysLeft = task.getDaysUntilDue();
        String message = String.format(
                "Reminder: Task '%s' is due in %d day(s)",
                task.title,
                daysLeft
        );

        LOG.warn("Notification to {}: {}", task.assignee.email, message);
    }

    /**
     * Уведомление о просроченной задаче
     */
    public void notifyOverdueTask(Task task) {
        if (task.assignee == null) return;

        String message = String.format(
                "Task '%s' is overdue! (Due date was: %s)",
                task.title,
                task.dueDate
        );

        LOG.error("Notification to {}: {}", task.assignee.email, message);
    }
}
