package com.taskmanagement.service;

import com.taskmanagement.model.entity.Task;
import com.taskmanagement.model.enums.TaskStatus;
import com.taskmanagement.model.repository.TaskRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ApplicationScoped
public class ScheduledTaskService {

    @Inject
    EntityManager em;

    @Inject
    TaskRepository taskRepository;

    @Inject
    NotificationService notificationService;

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTaskService.class);

    /**
     * Проверка и обновление просроченных задач (каждый час)
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void checkOverdueTasks() {
        LOG.info("Running overdue tasks check...");

        List<Task> overdueTasks = taskRepository.findOverdue();

        for (Task task : overdueTasks) {
            if (task.status != TaskStatus.OVERDUE) {
                task.status = TaskStatus.OVERDUE;
                em.merge(task);
                notificationService.notifyOverdueTask(task);
            }
        }

        LOG.info("Found {} overdue tasks", overdueTasks.size());
    }

    /**
     * Напоминания о задачах, срок которых истекает завтра (каждый день в 9:00)
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendUpcomingDeadlineReminders() {
        LOG.info("Sending deadline reminders...");

        List<Task> tasksDueSoon = taskRepository.findDueSoon(1); // Завтра

        for (Task task : tasksDueSoon) {
            notificationService.notifyUpcomingDeadline(task);
        }

        LOG.info("Sent {} deadline reminders", tasksDueSoon.size());
    }
}
