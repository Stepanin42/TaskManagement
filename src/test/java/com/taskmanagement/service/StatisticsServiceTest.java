package com.taskmanagement.service;

import com.taskmanagement.model.dto.TaskStatistics;
import com.taskmanagement.model.dto.UserStatistics;
import com.taskmanagement.model.entity.Task;
import com.taskmanagement.model.entity.User;
import com.taskmanagement.model.enums.TaskPriority;
import com.taskmanagement.model.enums.TaskStatus;
import com.taskmanagement.model.repository.TaskRepository;
import com.taskmanagement.model.repository.UserRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class StatisticsServiceTest {

    @Mock
    TaskRepository taskRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    StatisticsService statisticsService;


    private Task createTask(TaskStatus status) {
        Task task = new Task();
        task.status = status;
        task.title = "Task";
        task.priority = TaskPriority.MEDIUM;
        return task;
    }

    private Task createTaskOverdue() {
        Task task = new Task();
        task.status = TaskStatus.TODO;
        task.dueDate = LocalDate.now().minusDays(1);
        return task;
    }

    @Test
    void shouldCalculateTaskStatistics() {
        List<Task> tasks = Arrays.asList(
                createTask(TaskStatus.TODO),
                createTask(TaskStatus.TODO),
                createTask(TaskStatus.IN_PROGRESS),
                createTask(TaskStatus.DONE),
                createTask(TaskStatus.DONE),
                createTask(TaskStatus.DONE),
                createTask(TaskStatus.CANCELLED),
                createTaskOverdue()
        );
        Mockito.when(taskRepository.listAll()).thenReturn(tasks);

        TaskStatistics stats = statisticsService.getTaskStatistics();

        Assertions.assertAll(
                () -> Assertions.assertEquals(stats.getTotal(), 8),
                () -> Assertions.assertEquals(stats.getTodo(), 3),
                () -> Assertions.assertEquals(stats.getInProgress(), 1),
                () -> Assertions.assertEquals(stats.getDone(), 3),
                () -> Assertions.assertEquals(stats.getCancelled(), 1),
                () -> Assertions.assertEquals(stats.getOverdue(), 1),
                () -> Assertions.assertEquals(stats.getCompletionRate(), 37.5)
        );

    }

    private Task createCompletedTask(int id, long hoursToComplete) {
        Task task = new Task();
        task.id = (long) id;
        task.status = TaskStatus.DONE;
        LocalDateTime created = LocalDateTime.now().minusHours(hoursToComplete);
        task.createdAt = created;
        task.completedAt = LocalDateTime.now();
        task.dueDate = LocalDate.now().plusDays(1);
        return task;
    }

    @Test
    void shouldCalculateUserStatistics() {
        User user = new User();
        user.id = 1L;
        user.name = "John Doe";

        Task completedTask1 = createCompletedTask(1, 24); // Завершена за 24 часа
        Task completedTask2 = createCompletedTask(2, 48); // Завершена за 48 часов
        Task activeTask = createTask(TaskStatus.IN_PROGRESS);

        Mockito.when(userRepository.findById(1L)).thenReturn(user);
        Mockito.when(taskRepository.findByUser(1L)).thenReturn(Arrays.asList(
                completedTask1, completedTask2, activeTask
        ));

        UserStatistics stats = statisticsService.getUserStatistics(1L);

        Assertions.assertAll(
                () -> Assertions.assertEquals(stats.getUserName(), "John Doe"),
                () -> Assertions.assertEquals(stats.getTotalTasks(), 3),
                () -> Assertions.assertEquals(stats.getCompletedTasks(), 2),
                () -> Assertions.assertEquals(stats.getActiveTasks(), 1),
                () -> Assertions.assertEquals(stats.getAverageCompletionTimeHours(), 36.0)
        );
    }

    private Task createTaskWithPriority(TaskPriority priority, TaskStatus status) {
        Task task = new Task();
        task.priority = priority;
        task.status = status;
        return task;
    }

    @Test
    void shouldGroupTasksByPriority() {
        List<Task> tasks = Arrays.asList(
                createTaskWithPriority(TaskPriority.LOW, TaskStatus.TODO),
                createTaskWithPriority(TaskPriority.MEDIUM, TaskStatus.TODO),
                createTaskWithPriority(TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS),
                createTaskWithPriority(TaskPriority.HIGH, TaskStatus.TODO),
                createTaskWithPriority(TaskPriority.HIGH, TaskStatus.TODO),
                createTaskWithPriority(TaskPriority.HIGH, TaskStatus.TODO),
                createTaskWithPriority(TaskPriority.URGENT, TaskStatus.TODO)
        );

        Mockito.when(taskRepository.listAll()).thenReturn(tasks);

        Map<TaskPriority, Long> result = statisticsService.getTasksByPriority();

        Assertions.assertEquals(result.get(TaskPriority.LOW), 1L);
        Assertions.assertEquals(result.get(TaskPriority.MEDIUM), 2L);
        Assertions.assertEquals(result.get(TaskPriority.HIGH), 3L);
        Assertions.assertEquals(result.get(TaskPriority.URGENT), 1L);
    }
}
