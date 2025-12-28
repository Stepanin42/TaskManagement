package com.taskmanagement.service;

import com.taskmanagement.model.entity.Task;
import com.taskmanagement.model.entity.User;
import com.taskmanagement.model.enums.TaskStatus;
import com.taskmanagement.model.repository.TaskRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class ScheduledTaskServiceTest {

    @Mock
    TaskRepository taskRepository;

    @Mock
    NotificationService notificationService;

    @Mock
    EntityManager entityManager;

    @InjectMocks
    ScheduledTaskService scheduledTaskService;

    @Test
    void shouldMarkOverdueTasksAndNotify() {
        Task overdueTask1 = createOverdueTask(1L, "Overdue 1", TaskStatus.TODO);
        Task overdueTask2 = createOverdueTask(2L, "Overdue 2", TaskStatus.IN_PROGRESS);

        Mockito.when(taskRepository.findOverdue()).thenReturn(Arrays.asList(overdueTask1, overdueTask2));
        Mockito.when(entityManager.merge(Mockito.any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        scheduledTaskService.checkOverdueTasks();

        Mockito.verify(notificationService, Mockito.times(2)).notifyOverdueTask(Mockito.any(Task.class));
    }

    @Test
    void shouldNotUpdateAlreadyOverdueTasks() {
        Task alreadyOverdue = createOverdueTask(1L, "Already Overdue", TaskStatus.OVERDUE);

        Mockito.when(taskRepository.findOverdue()).thenReturn(Arrays.asList(alreadyOverdue));

        scheduledTaskService.checkOverdueTasks();

        Mockito.verify(entityManager, Mockito.never()).merge(Mockito.any(Task.class));
        Mockito.verify(notificationService, Mockito.never()).notifyOverdueTask(Mockito.any(Task.class));
    }

    @Test
    void shouldSendDeadlineReminders() {
        Task dueTomorrow1 = createTaskDueSoon(1L, "Due Tomorrow 1");
        Task dueTomorrow2 = createTaskDueSoon(2L, "Due Tomorrow 2");

        Mockito.when(taskRepository.findDueSoon(1)).thenReturn(Arrays.asList(dueTomorrow1, dueTomorrow2));

        scheduledTaskService.sendUpcomingDeadlineReminders();

        Mockito.verify(notificationService, Mockito.times(2)).notifyUpcomingDeadline(Mockito.any(Task.class));
        Mockito.verify(notificationService).notifyUpcomingDeadline(dueTomorrow1);
        Mockito.verify(notificationService).notifyUpcomingDeadline(dueTomorrow2);
    }

    @Test
    void shouldCheckOverdueTasksWithMockedTime() {
        Task task = new Task();
        task.id = 1L;
        task.title = "Task";
        task.status = TaskStatus.TODO;
        task.dueDate = LocalDate.of(2025, 12, 26); // Вчера

        Mockito.when(taskRepository.findOverdue()).thenReturn(List.of(task));
        Mockito.when(entityManager.merge(Mockito.any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        scheduledTaskService.checkOverdueTasks();

        Mockito.verify(notificationService, Mockito.times(1)).notifyOverdueTask(Mockito.any(Task.class));
    }

    private Task createOverdueTask(Long id, String title, TaskStatus status) {
        Task task = new Task();
        task.id = id;
        task.title = title;
        task.status = status;
        task.dueDate = LocalDate.now().minusDays(1);
        return task;
    }

    private Task createTaskDueSoon(Long id, String title) {
        Task task = new Task();
        task.id = id;
        task.title = title;
        task.status = TaskStatus.TODO;
        task.dueDate = LocalDate.now().plusDays(1);

        User user = new User();
        user.email = "user@example.com";
        task.assignee = user;

        return task;
    }
}
