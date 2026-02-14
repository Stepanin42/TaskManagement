package com.taskmanagement.service;

import com.taskmanagement.model.dto.TaskStatistics;
import com.taskmanagement.model.dto.UserStatistics;
import com.taskmanagement.model.entity.Task;
import com.taskmanagement.model.entity.User;
import com.taskmanagement.model.enums.TaskPriority;
import com.taskmanagement.model.enums.TaskStatus;
import com.taskmanagement.model.repository.TaskRepository;
import com.taskmanagement.model.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class StatisticsService {
    @Inject
    TaskRepository taskRepository;

    @Inject
    UserRepository userRepository;

    private final static Logger LOG = LoggerFactory.getLogger(StatisticsService.class);

    /**
     * Статистика по задачам
     */
    public TaskStatistics getTaskStatistics() {
        List<Task> allTasks = taskRepository.listAll();

        long total = allTasks.size();
        long todo = allTasks.stream().filter(t -> t.status == TaskStatus.TODO).count();
        long inProgress = allTasks.stream().filter(t -> t.status == TaskStatus.IN_PROGRESS).count();
        long done = allTasks.stream().filter(t -> t.status == TaskStatus.DONE).count();
        long cancelled = allTasks.stream().filter(t -> t.status == TaskStatus.CANCELLED).count();
        long overdue = allTasks.stream().filter(Task::isOverdue).count();

        return new TaskStatistics(total, todo, inProgress, done, cancelled, overdue);
    }

    /**
     * Статистика по пользователю
     */
    public UserStatistics getUserStatistics(Long userId) {
        LOG.info("Get user statistics for {}", userId);
        MDC.put("userId", String.valueOf(userId));

        try {
            User user = Optional.of(userRepository.findById(userId))
                    .orElseThrow(() -> new NotFoundException("User not found"));

            List<Task> userTasks = taskRepository.findByUser(userId);

            long totalTasks = userTasks.size();
            long completedTasks = userTasks.stream()
                    .filter(t -> t.status == TaskStatus.DONE)
                    .count();
            long activeTasks = userTasks.stream()
                    .filter(t -> t.status != TaskStatus.DONE && t.status != TaskStatus.CANCELLED)
                    .count();

            // Средняя продолжительность выполнения
            double averageCompletionTime = userTasks.stream()
                    .filter(t -> t.status == TaskStatus.DONE && t.completedAt != null)
                    .mapToLong(t -> Duration.between(t.createdAt, t.completedAt).toHours())
                    .average()
                    .orElse(0.0);

            // Процент выполнения в срок
            long completedOnTime = userTasks.stream()
                    .filter(t -> t.status == TaskStatus.DONE
                            && t.dueDate != null
                            && t.completedAt != null
                            && !t.completedAt.toLocalDate().isAfter(t.dueDate))
                    .count();

            double onTimePercentage = completedTasks > 0
                    ? (completedOnTime * 100.0 / completedTasks)
                    : 0.0;

            return new UserStatistics(
                    user.name,
                    totalTasks,
                    completedTasks,
                    activeTasks,
                    averageCompletionTime,
                    onTimePercentage
            );

        } catch (NotFoundException e) {
            LOG.error("User statistic error not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.error("User statistics error: {}", e.getMessage());
            throw e;
        } finally {
            MDC.remove("userId");
        }
    }

    /**
     * Распределение задач по приоритетам
     */
    public Map<TaskPriority, Long> getTasksByPriority() {
        List<Task> allTasks = taskRepository.listAll();

        return allTasks.stream()
                .filter(t -> t.status != TaskStatus.DONE && t.status != TaskStatus.CANCELLED)
                .collect(Collectors.groupingBy(t -> t.priority, Collectors.counting()));
    }
}
