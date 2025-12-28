package com.taskmanagement.model.repository;

import com.taskmanagement.model.entity.Task;
import com.taskmanagement.model.entity.User;
import com.taskmanagement.model.enums.TaskPriority;
import com.taskmanagement.model.enums.TaskStatus;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@QuarkusTest
@TestProfile(RepositoryTestProfile.class)
public class TaskRepositoryTest {

    @Inject
    TaskRepository taskRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    EntityManager em;

    private User testUser;

    @BeforeEach
    @Transactional
    void setUp() {
        // Очистка БД
        em.createQuery("DELETE FROM Task").executeUpdate();
        em.createQuery("DELETE FROM User").executeUpdate();

        // Создание тестового пользователя
        testUser = new User();
        testUser.name = "Test User";
        testUser.email = "test@example.com";
        testUser = userRepository.save(testUser);
    }

    @Test
    @Transactional
    void shouldSaveAndRetrieveTask() {
        Task task = new Task();
        task.title = "Database Test Task";
        task.description = "Testing repository";
        task.status = TaskStatus.TODO;
        task.priority = TaskPriority.MEDIUM;
        task.assignee = testUser;

        Task saved = em.merge(task);
        em.flush();
        em.clear();

        Task retrieved = taskRepository.findById(saved.id);

        Assertions.assertNotNull(retrieved);
        Assertions.assertEquals(task.title, "Database Test Task");
        Assertions.assertEquals(retrieved.assignee.id, testUser.id);
    }

    @Test
    @Transactional
    void shouldFindOverdueTasks() {
        Task overdueTask1 = createTask("Overdue 1", TaskStatus.TODO);
        overdueTask1.dueDate = LocalDate.now().minusDays(5);

        Task overdueTask2 = createTask("Overdue 2", TaskStatus.IN_PROGRESS);
        overdueTask2.dueDate = LocalDate.now().minusDays(1);

        Task futureTask = createTask("Future Task", TaskStatus.TODO);
        futureTask.dueDate = LocalDate.now().plusDays(5);

        Task completedOverdue = createTask("Completed", TaskStatus.DONE);
        completedOverdue.dueDate = LocalDate.now().minusDays(2);

        em.flush();

        List<Task> overdueTasks = taskRepository.findOverdue();

        Assertions.assertEquals(2, overdueTasks.size());
    }

    @Test
    @Transactional
    void shouldFindTasksDueSoon() {
        Task dueTomorrow = createTask("Due Tomorrow", TaskStatus.TODO);
        dueTomorrow.dueDate = LocalDate.now().plusDays(1);

        Task dueInWeek = createTask("Due in Week", TaskStatus.TODO);
        dueInWeek.dueDate = LocalDate.now().plusDays(7);

        Task dueFarFuture = createTask("Due Far", TaskStatus.TODO);
        dueFarFuture.dueDate = LocalDate.now().plusDays(30);

        em.flush();

        List<Task> dueSoon = taskRepository.findDueSoon(7);

        Assertions.assertEquals(dueSoon.size(), 2);
    }

    @Test
    @Transactional
    void shouldFindTasksByUser() {
        User anotherUser = new User();
        anotherUser.name = "Another User";
        anotherUser.email = "another@example.com";
        anotherUser = userRepository.save(anotherUser);

        Task task1 = createTask("User 1 Task 1", TaskStatus.TODO);
        task1.assignee = testUser;

        Task task2 = createTask("User 1 Task 2", TaskStatus.TODO);
        task2.assignee = testUser;

        Task task3 = createTask("User 2 Task", TaskStatus.TODO);
        task3.assignee = anotherUser;

        em.flush();

        List<Task> user1Tasks = taskRepository.findByUser(testUser.id);

        Assertions.assertEquals(2, user1Tasks.size());
    }

    @Test
    @Transactional
    void shouldUpdateTask() {
        Task task = createTask("Original Title", TaskStatus.TODO);
        em.flush();

        Long taskId = task.id;

        task.title = "Updated Title";
        task.status = TaskStatus.IN_PROGRESS;
        em.merge(task);
        em.flush();
        em.clear();

        Task updated = taskRepository.findById(taskId);
        Assertions.assertNotNull(updated);
        Assertions.assertEquals(updated.title, "Updated Title");
        Assertions.assertEquals(updated.status, TaskStatus.IN_PROGRESS);
    }

    @Test
    @Transactional
    void shouldDeleteTask() {
        Task task = createTask("Task to delete", TaskStatus.TODO);
        em.flush();

        Long taskId = task.id;

        taskRepository.deleteById(taskId);
        em.flush();

        Task deleted = taskRepository.findById(taskId);
        Assertions.assertNull(deleted);
    }

    @Test
    @Transactional
    void shouldNotDeleteUserWhenDeletingTask() {
        Task task = createTask("Task", TaskStatus.TODO);
        task.assignee = testUser;
        em.flush();

        Long taskId = task.id;
        Long userId = testUser.id;

        taskRepository.deleteById(taskId);
        em.flush();

        User user = userRepository.findById(userId);
        Assertions.assertNotNull(user);
    }

    private Task createTask(String title, TaskStatus status) {
        Task task = new Task();
        task.title = title;
        task.status = status;
        task.priority = TaskPriority.MEDIUM;
        return em.merge(task);
    }
}

