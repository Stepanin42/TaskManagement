package com.taskmanagement.service;

import com.taskmanagement.model.dto.TaskCreateRequest;
import com.taskmanagement.model.entity.Category;
import com.taskmanagement.model.entity.Task;
import com.taskmanagement.model.entity.User;
import com.taskmanagement.model.enums.TaskPriority;
import com.taskmanagement.model.enums.TaskStatus;
import com.taskmanagement.model.repository.CategoryRepository;
import com.taskmanagement.model.repository.TaskRepository;
import com.taskmanagement.model.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;

import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class TaskService {

    @Inject
    EntityManager em;

    @Inject
    TaskRepository taskRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    NotificationService notificationService;

    @Inject
    Validator validator;

    private static final Logger LOG = LoggerFactory.getLogger(TaskService.class);

    //Создание задачи
    @Transactional
    public Task createTask(TaskCreateRequest request) throws ValidationException {
        long startTime = System.currentTimeMillis();
        LOG.info("Creating task");
        LOG.trace("Create task request: {}", request.toString());
        // Валидация
        validateTaskRequest(request);
        LOG.debug("Task validate");

        Task task = new Task();
        try {
            if (request.getTitle() == null || request.getTitle().isEmpty()) {
                LOG.error("Title is empty");
                throw new ValidationException("Title is required");
            }
            task.title = request.getTitle();
            task.description = request.getDescription();
            task.priority = request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM;
            task.dueDate = request.getDueDate();
            task.estimatedHours = request.getEstimatedHours();

            // Назначение пользователя
            if (request.getAssigneeId() != null) {
                task.assignee = Optional.ofNullable(userRepository.findById(request.getAssigneeId()))
                        .orElseThrow(() -> new NotFoundException("User not found"));
            }

            // Категория
            if (request.getCategoryId() != null) {
                Category category = Optional.of(categoryRepository.findById(request.getCategoryId()))
                        .orElseThrow(() -> new NotFoundException("Category not found"));
                task.category = category;
            }

            Task saved = em.merge(task);

            // Уведомление
            if (saved.assignee != null) {
                notificationService.notifyTaskAssigned(saved);
            }

            LOG.info("Created task: {} (ID: {})", saved.title, saved.id);
            long endTime = System.currentTimeMillis();
            LOG.info("Task created in {} ms", endTime - startTime);

            return saved;

        } catch (ValidationException e){
            LOG.error("Task create validate error: {}", e.getMessage());
            throw e;
        } catch (NotFoundException e){
            LOG.error("Task create not found error: {}", e.getMessage());
            throw e;
        } catch (Exception e){
            LOG.error("Task create unexpected error: {}", e.getMessage());
            throw e;
        }
    }

    //Обновление статуса задачи
    @Transactional
    public Task updateStatus(Long taskId, TaskStatus newStatus) {
        LOG.info("Updating status of task: {} to {}", taskId, newStatus);
        MDC.put("taskId", String.valueOf(taskId));

        try {
            Task task = getTaskById(taskId);

            if (!task.canTransitionTo(newStatus)) {
                throw new RuntimeException(
                        String.format("Cannot transition from %s to %s", task.status, newStatus));
            }

            TaskStatus oldStatus = task.status;
            task.status = newStatus;

            if (newStatus == TaskStatus.DONE) {
                task.complete();
            }

            Task updated = em.merge(task);

            LOG.info("Updated task #{} status: {} -> {}", taskId, oldStatus, newStatus);

            // Уведомление
            notificationService.notifyStatusChanged(updated, oldStatus);

            return updated;
        } catch (Exception e){
            LOG.error("Task update unexpected error: {}", e.getMessage());
            throw e;
        } finally {
            MDC.remove("taskId");
        }
    }

    //Обновление приоритета
    @Transactional
    public Task updatePriority(Long taskId, TaskPriority newPriority) {
        Task task = getTaskById(taskId);
        TaskPriority oldPriority = task.priority;

        task.priority = newPriority;
        Task updated = em.merge(task);

        LOG.info("Updated task #{} priority: {} -> {}", taskId, oldPriority, newPriority);

        return updated;
    }

    //Назначение задачи пользователю
    @Transactional
    public Task assignTask(Long taskId, Long userId) {
        LOG.info("Assigning task {} to user {}", taskId, userId);
        MDC.put("taskId", String.valueOf(taskId));
        MDC.put("userId", String.valueOf(userId));
        try {
            Task task = getTaskById(taskId);
            User user = Optional.of(userRepository.findById(userId))
                    .orElseThrow(() -> new NotFoundException("User not found"));

            User previousAssignee = task.assignee;
            task.assignee = user;

            Task updated = em.merge(task);

            // Уведомление новому пользователю
            notificationService.notifyTaskAssigned(updated);

            if (previousAssignee != null) {
                notificationService.notifyTaskUnassigned(updated, previousAssignee);
            }

            LOG.info("Assigned task #{} to user {}", taskId, user.email);

            return updated;
        } catch (NotFoundException e){
            LOG.error("Assign task error not found: {}", e.getMessage());
            throw e;
        } catch (Exception e){
            LOG.error("Assign task unexpected error: {}", e.getMessage());
            throw e;
        } finally {
            MDC.remove("taskId");
            MDC.remove("userId");
        }
    }

    //Получение задачи по ID
    public Task getTaskById(Long id) {
        return Optional.ofNullable(taskRepository.findById(id))
                .orElseThrow(() -> new NotFoundException("Task not found: " + id));
    }

    //Получение всех задач
    public List<Task> getAllTasks() {
        return taskRepository.listAll();
    }

    //Фильтрация задач
    public List<Task> filterTasks(TaskStatus status, TaskPriority priority, Long userId) {
        List<Task> tasks = taskRepository.listAll();

        if (status != null) {
            tasks = tasks.stream()
                    .filter(t -> t.status == status)
                    .collect(Collectors.toList());
        }

        if (priority != null) {
            tasks = tasks.stream()
                    .filter(t -> t.priority == priority)
                    .collect(Collectors.toList());
        }

        if (userId != null) {
            tasks = tasks.stream()
                    .filter(t -> t.assignee != null && t.assignee.id.equals(userId))
                    .collect(Collectors.toList());
        }

        return tasks;
    }

    //Получение просроченных задач
    public List<Task> getOverdueTasks() {
        return taskRepository.findOverdue();
    }

    //Получение задач срок которых истекает скоро
    public List<Task> getTasksDueSoon(int days) {
        return taskRepository.findDueSoon(days);
    }


    //Удаление задачи
    @Transactional
    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        taskRepository.deleteById(id);

        LOG.info("Deleted task: {} (ID: {})", task.title, id);
    }

    private void validateTaskRequest(TaskCreateRequest request) throws ValidationException {
        Set<ConstraintViolation<TaskCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ValidationException("Invalid task request");
        }

        if (request.getDueDate() != null && request.getDueDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Due date cannot be in the past");
        }
    }
}
