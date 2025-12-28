package com.taskmanagement.model.entity;

import com.taskmanagement.model.enums.TaskPriority;
import com.taskmanagement.model.enums.TaskStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
public class TaskTest {

    @Test
    void shouldBeOverdueWhenDueDatePassed() {
        Task task = new Task();
        task.dueDate = LocalDate.now().minusDays(1);
        task.status = TaskStatus.TODO;

        Assertions.assertTrue(task.isOverdue());
    }

    @Test
    void shouldNotBeOverdueWhenCompleted() {
        Task task = new Task();
        task.dueDate = LocalDate.now().minusDays(1);
        task.status = TaskStatus.DONE;

        Assertions.assertFalse(task.isOverdue());
    }

    @ParameterizedTest
    @CsvSource({
            "1, 1",
            "7, 7",
            "30, 30",
            "-1, -1"
    })
    void shouldCalculateDaysUntilDue(int daysFromNow, long expected) {
        Task task = new Task();
        task.dueDate = LocalDate.now().plusDays(daysFromNow);

        long result = task.getDaysUntilDue();

        Assertions.assertEquals(expected, result);
    }

    @Test
    void shouldBeUrgentWhenPriorityIsUrgent() {
        Task task = new Task();
        task.priority = TaskPriority.URGENT;
        task.dueDate = LocalDate.now().plusDays(10);

        Assertions.assertTrue(task.isUrgent());
    }

    @Test
    void shouldBeUrgentWhenDueWithinOneDay() {
        Task task = new Task();
        task.priority = TaskPriority.MEDIUM;
        task.dueDate = LocalDate.now().plusDays(1);

        Assertions.assertTrue(task.isUrgent());
    }

    @Test
    void shouldCompleteTask() {
        Task task = new Task();
        task.status = TaskStatus.IN_PROGRESS;

        LocalDateTime before = LocalDateTime.now().minusNanos(1);

        //Очень быстро выполняется before = completedAt = after
        task.complete();

        LocalDateTime after = LocalDateTime.now().plusNanos(1);

        Assertions.assertEquals(task.status, TaskStatus.DONE);
        Assertions.assertTrue(task.completedAt.isAfter(before));
        Assertions.assertTrue(task.completedAt.isBefore(after));
    }

    private static Stream<Arguments> provideValidTransitions() {
        return Stream.of(
                Arguments.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS),
                Arguments.of(TaskStatus.TODO, TaskStatus.CANCELLED),
                Arguments.of(TaskStatus.IN_PROGRESS, TaskStatus.DONE),
                Arguments.of(TaskStatus.IN_PROGRESS, TaskStatus.TODO),
                Arguments.of(TaskStatus.DONE, TaskStatus.TODO)
        );
    }

    @ParameterizedTest
    @MethodSource("provideValidTransitions")
    void shouldAllowValidTransitions(TaskStatus from, TaskStatus to) {
        Task task = new Task();
        task.status = from;

        Assertions.assertTrue(task.canTransitionTo(to));
    }

    private static Stream<Arguments> provideInvalidTransitions() {
        return Stream.of(
                Arguments.of(TaskStatus.TODO, TaskStatus.DONE),
                Arguments.of(TaskStatus.DONE, TaskStatus.IN_PROGRESS),
                Arguments.of(TaskStatus.CANCELLED, TaskStatus.DONE)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidTransitions")
    void shouldRejectInvalidTransitions(TaskStatus from, TaskStatus to) {
        Task task = new Task();
        task.status = from;

        Assertions.assertFalse(task.canTransitionTo(to));
    }
}
