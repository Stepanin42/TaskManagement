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
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    TaskRepository taskRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    NotificationService notificationService;

    @Mock
    EntityManager entityManager;

    @Mock
    Validator validator;

    @InjectMocks
    TaskService taskService;

    private Task testTask;
    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.id = 1L;
        testUser.name = "Vanechka";
        testUser.email = "testovich@example.com";
        testUser.active = true;

        testCategory = new Category();
        testCategory.id = 1L;
        testCategory.name = "Work";

        testTask = new Task();
        testTask.id = 1L;
        testTask.title = "Test Task";
        testTask.description = "Test Description";
        testTask.status = TaskStatus.TODO;
        testTask.priority = TaskPriority.MEDIUM;
        testTask.assignee = testUser;
        testTask.category = testCategory;
        testTask.dueDate = LocalDate.now().plusDays(7);
    }

    @Test
    void shouldCreateTaskSuccessfully() throws ValidationException {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("New Task");
        request.setDescription("Description");
        request.setPriority(TaskPriority.HIGH);
        request.setAssigneeId(1L);
        request.setCategoryId(1L);

        Mockito.when(validator.validate(request)).thenReturn(Collections.emptySet());
        Mockito.when(userRepository.findById(1L)).thenReturn(testUser);
        Mockito.when(categoryRepository.findById(1L)).thenReturn(testCategory);
        Mockito.when(entityManager.merge(Mockito.any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.id = 1L;
            return task;
        });

        Task result = taskService.createTask(request);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.title, "New Task");
        Assertions.assertEquals(result.description, "Description");
        Assertions.assertEquals(result.priority, TaskPriority.HIGH);
        Assertions.assertEquals(result.assignee, testUser);

        Mockito.verify(entityManager, Mockito.times(1)).merge(Mockito.any(Task.class));
        Mockito.verify(notificationService, Mockito.times(1)).notifyTaskAssigned(Mockito.any(Task.class));
        Mockito.verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void shouldThrowExceptionWhenTaskNotFound() {

        Mockito.when(taskRepository.findById(999L)).thenReturn(null);

        NotFoundException notFoundException = Assertions.assertThrows(NotFoundException.class, () -> taskService.getTaskById(999L));

        Mockito.verify(taskRepository).findById(999L);
        Assertions.assertEquals(notFoundException.getMessage(), "Task not found: 999");
    }

    @Test
    void shouldSaveTaskWithCorrectValues() throws ValidationException {
        // Given
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Captured Task");
        request.setEstimatedHours(5);

        Mockito.when(validator.validate(request)).thenReturn(Collections.emptySet());
        Mockito.when(entityManager.merge(Mockito.any(Task.class))).thenReturn(testTask);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);

        // When
        taskService.createTask(request);

        // Then
        Mockito.verify(entityManager).merge(taskCaptor.capture());

        Task capturedTask = taskCaptor.getValue();
        Assertions.assertEquals(capturedTask.title, "Captured Task");
        Assertions.assertEquals(capturedTask.estimatedHours, 5);
        Assertions.assertEquals(capturedTask.priority, TaskPriority.MEDIUM);
        Assertions.assertEquals(capturedTask.status, TaskStatus.TODO);
    }

    @ParameterizedTest
    @CsvSource({
            "LOW, 1",
            "MEDIUM, 2",
            "HIGH, 3",
            "URGENT, 4"
    })
    void shouldHaveCorrectPriorityLevel(TaskPriority priority, int expectedLevel) {
        Assertions.assertEquals(priority.getLevel(), expectedLevel);
    }

    private static Stream<Arguments> provideInvalidStatusTransitions() {
        return Stream.of(
                Arguments.of(TaskStatus.TODO, TaskStatus.DONE),
                Arguments.of(TaskStatus.DONE, TaskStatus.IN_PROGRESS),
                Arguments.of(TaskStatus.CANCELLED, TaskStatus.DONE)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidStatusTransitions")
    void shouldRejectInvalidStatusTransitions(TaskStatus from, TaskStatus to) {
        testTask.status = from;
        Mockito.when(taskRepository.findById(1L)).thenReturn(testTask);

        Assertions.assertThrows(RuntimeException.class, () -> taskService.updateStatus(1L, to));
    }

    // ========== ТЕСТ 7: Nested тесты для группировки ==========
    @Nested
    class UpdateStatusTests {

        @Test
        void shouldUpdateStatusToInProgress() {
            testTask.status = TaskStatus.TODO;
            Mockito.when(taskRepository.findById(1L)).thenReturn(testTask);
            Mockito.when(entityManager.merge(Mockito.any(Task.class))).thenReturn(testTask);

            Task result = taskService.updateStatus(1L, TaskStatus.IN_PROGRESS);

            Assertions.assertEquals(result.status, TaskStatus.IN_PROGRESS);
            Mockito.verify(notificationService).notifyStatusChanged(Mockito.any(Task.class), Mockito.eq(TaskStatus.TODO));
        }

        @Test
        void shouldCompleteTask() {
            testTask.status = TaskStatus.IN_PROGRESS;
            Mockito.when(taskRepository.findById(1L)).thenReturn(testTask);
            Mockito.when(entityManager.merge(Mockito.any(Task.class))).thenReturn(testTask);

            Task result = taskService.updateStatus(1L, TaskStatus.DONE);

            Assertions.assertEquals(result.status, TaskStatus.DONE);
            Assertions.assertNotNull(result.completedAt);
        }
    }

    @Test
    void shouldHandleNotificationFailure() {
        // Given
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Task");
        request.setAssigneeId(1L);

        Mockito.when(validator.validate(request)).thenReturn(Collections.emptySet());
        Mockito.when(userRepository.findById(1L)).thenReturn(testUser);
        Mockito.when(entityManager.merge(Mockito.any(Task.class))).thenReturn(testTask);

        Mockito.doThrow(new RuntimeException("Email service down"))
                .when(notificationService).notifyTaskAssigned(Mockito.any(Task.class));

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> taskService.createTask(request));

        Assertions.assertEquals("Email service down", exception.getMessage());
    }

    @Test
    void shouldCallNotificationService() {
        testTask.status = TaskStatus.TODO;
        Mockito.when(taskRepository.findById(1L)).thenReturn(testTask);
        Mockito.when(entityManager.merge(Mockito.any(Task.class))).thenReturn(testTask);

        Mockito.doNothing().when(notificationService).notifyStatusChanged(Mockito.any(), Mockito.any());

        taskService.updateStatus(1L, TaskStatus.IN_PROGRESS);

        Mockito.verify(notificationService, Mockito.times(1))
                .notifyStatusChanged(Mockito.any(Task.class), Mockito.eq(TaskStatus.TODO));
    }

    @Test
    @DisplayName("Should call repositories in correct order")
    void shouldCallRepositoriesInOrder() throws ValidationException {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Task");
        request.setAssigneeId(1L);
        request.setCategoryId(1L);

        Mockito.when(validator.validate(request)).thenReturn(Collections.emptySet());
        Mockito.when(userRepository.findById(1L)).thenReturn(testUser);
        Mockito.when(categoryRepository.findById(1L)).thenReturn(testCategory);
        Mockito.when(entityManager.merge(Mockito.any(Task.class))).thenReturn(testTask);

        InOrder inOrder = Mockito.inOrder(userRepository, categoryRepository, entityManager);

        taskService.createTask(request);

        inOrder.verify(userRepository).findById(1L);
        inOrder.verify(categoryRepository).findById(1L);
        inOrder.verify(entityManager).merge(Mockito.any(Task.class));
    }

    @Test
    void shouldNotNotifyWhenAssigneeIsNull() throws ValidationException {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Unassigned Task");
        testTask.assignee = null;

        Mockito.when(validator.validate(request)).thenReturn(Collections.emptySet());
        Mockito.when(entityManager.merge(Mockito.any(Task.class))).thenReturn(testTask);

        taskService.createTask(request);

        Mockito.verify(notificationService, Mockito.never()).notifyTaskAssigned(Mockito.any());
    }

    @Test
    @DisplayName("Should assign task and notify both users")
    void shouldAssignTaskAndNotifyBothUsers() {
        User oldUser = new User();
        oldUser.id = 1L;
        oldUser.email = "old@example.com";

        User newUser = new User();
        newUser.id = 2L;
        newUser.email = "new@example.com";

        testTask.assignee = oldUser;

        Mockito.when(taskRepository.findById(1L)).thenReturn(testTask);
        Mockito.when(userRepository.findById(2L)).thenReturn(newUser);
        Mockito.when(entityManager.merge(Mockito.any(Task.class))).thenReturn(testTask);

        taskService.assignTask(1L, 2L);

        Mockito.verify(notificationService).notifyTaskAssigned(Mockito.any(Task.class));
        Mockito.verify(notificationService).notifyTaskUnassigned(Mockito.any(Task.class), Mockito.eq(oldUser));
    }

    @Test
    @DisplayName("Should use argument matchers correctly")
    void shouldUseArgumentMatchers() {
        Mockito.when(taskRepository.findById(Mockito.anyLong())).thenReturn(testTask);
        Mockito.when(entityManager.merge(Mockito.any(Task.class))).thenReturn(testTask);

        taskService.updatePriority(1L, TaskPriority.URGENT);

        Mockito.verify(taskRepository).findById(Mockito.eq(1L));
    }

    @Test
    @DisplayName("Should create task with all properties set")
    void shouldCreateTaskWithAllProperties() throws ValidationException {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Complete Task");
        request.setDescription("Full description");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(3));
        request.setEstimatedHours(8);
        request.setAssigneeId(1L);
        request.setCategoryId(1L);

        Mockito.when(validator.validate(request)).thenReturn(Collections.emptySet());
        Mockito.when(userRepository.findById(1L)).thenReturn(testUser);
        Mockito.when(categoryRepository.findById(1L)).thenReturn(testCategory);
        Mockito.when(entityManager.merge(Mockito.any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.createTask(request);

        Assertions.assertAll(
                () -> Assertions.assertEquals(result.title, "Complete Task"),
                () -> Assertions.assertEquals(result.description, "Full description"),
                () -> Assertions.assertEquals(result.priority, TaskPriority.HIGH),
                () -> Assertions.assertEquals(result.dueDate, LocalDate.now().plusDays(3)),
                () -> Assertions.assertEquals(result.estimatedHours, 8),
                () -> Assertions.assertEquals(result.assignee, testUser),
                () -> Assertions.assertEquals(result.category, testCategory),
                () -> Assertions.assertEquals(result.status, TaskStatus.TODO)

        );
    }

    @RepeatedTest(5)
    void shouldCreateUniqueTasksRepeated(RepetitionInfo repetitionInfo) throws ValidationException {
        String title = "Task #" + repetitionInfo.getCurrentRepetition();
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle(title);

        Mockito.when(validator.validate(request)).thenReturn(Collections.emptySet());
        Mockito.when(entityManager.merge(Mockito.any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.createTask(request);

        Assertions.assertEquals(result.title, title);
    }

    @Test
    void shouldThrowValidationException() {
        TaskCreateRequest request = new TaskCreateRequest();

        ConstraintViolation<TaskCreateRequest> violation = Mockito.mock(ConstraintViolation.class);
        Set<ConstraintViolation<TaskCreateRequest>> violations = Set.of(violation);

        Mockito.when(validator.validate(request)).thenReturn(violations);

        // When & Then
        ValidationException validationException = Assertions.assertThrows(ValidationException.class, () -> taskService.createTask(request));
        Assertions.assertEquals(validationException.getMessage(), "Invalid task request");
    }

    @ParameterizedTest
    @NullSource
    void shouldHandleNullAssignee(Long assigneeId) throws ValidationException {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Task without assignee");
        request.setAssigneeId(assigneeId);

        Mockito.when(validator.validate(request)).thenReturn(Collections.emptySet());
        Mockito.when(entityManager.merge(Mockito.any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.createTask(request);

        Assertions.assertNull(result.assignee);
        Mockito.verify(notificationService, Mockito.never()).notifyTaskAssigned(Mockito.any());
    }
}
