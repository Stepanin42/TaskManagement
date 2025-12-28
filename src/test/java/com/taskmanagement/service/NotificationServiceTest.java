package com.taskmanagement.service;

import com.taskmanagement.model.entity.Task;
import com.taskmanagement.model.entity.User;
import com.taskmanagement.model.enums.TaskPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Spy
    @InjectMocks
    NotificationService notificationService;

    @Test
    void shouldUseRealMethodButSpy() {
        Task task = new Task();
        task.title = "Test Task";
        task.priority = TaskPriority.HIGH;
        task.dueDate = LocalDate.now().plusDays(7);

        User user = new User();
        user.email = "user@example.com";
        task.assignee = user;

        notificationService.notifyTaskAssigned(task);

        Mockito.verify(notificationService, Mockito.times(1)).notifyTaskAssigned(task);
    }

    @Test
    void shouldOverrideMethodWithDoReturn() {
        Task task = new Task();
        task.title = "Task";

        User user = new User();
        user.email = "user@example.com";
        task.assignee = user;

        Mockito.doNothing().when(notificationService).notifyTaskAssigned(Mockito.any(Task.class));

        notificationService.notifyTaskAssigned(task);

        Mockito.verify(notificationService).notifyTaskAssigned(task);
    }
}
