package com.taskmanagement.resource;

import com.taskmanagement.model.dto.TaskCreateRequest;
import com.taskmanagement.model.dto.UserCreateDto;
import com.taskmanagement.model.entity.User;
import com.taskmanagement.model.enums.TaskPriority;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskResourceTest {

    private static int createdTaskId;
    private static int createdUserId;

    @Test
    @Order(1)//Приоритет
    void shouldCreateTask() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Integration Test Task");
        request.setDescription("Created via REST API");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(7));

        createdTaskId = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/tasks")
                .then()
                .statusCode(201)
                .body("title", equalTo("Integration Test Task")) //Сравнение полей
                .body("priority", equalTo("HIGH"))
                .body("status", equalTo("TODO"))
                .body("description", equalTo("Created via REST API"))
                .extract()
                .path("id");//Берем одно поле в переменную

        Assertions.assertNotNull(createdTaskId);
    }

    @Test
    @Order(2)
    void shouldGetAllTasks() {
        given()
                .when()
                .get("/api/tasks")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].title", notNullValue())
                .body("[0].status", notNullValue());
    }

    @Test
    @Order(3)
    void shouldGetTaskById() {
        given()
                .pathParam("id", createdTaskId)
                .when()
                .get("/api/tasks/{id}")
                .then()
                .statusCode(200)
                .body("id", equalTo(createdTaskId))
                .body("title", equalTo("Integration Test Task"))
                .body("priority", equalTo("HIGH"));
    }

    @Test
    @Order(4)
    void shouldReturn404ForNonExistentTask() {
        given()
                .pathParam("id", 99999)
                .when()
                .get("/api/tasks/{id}")
                .then()
                .statusCode(404)
                .body(containsString("Task not found"));
    }

    @Test
    @Order(5)
    void shouldUpdateTaskStatus() {
        given()
                .pathParam("id", createdTaskId)
                .queryParam("status", "IN_PROGRESS")
                .when()
                .put("/api/tasks/{id}/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("IN_PROGRESS"));
    }

    @Test
    @Order(6)
    void shouldRejectInvalidStatusTransition() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Task for invalid transition");

        Integer taskId = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/tasks")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        given()
                .pathParam("id", taskId)
                .queryParam("status", "DONE")
                .when()
                .put("/api/tasks/{id}/status")
                .then()
                .statusCode(500)
                .body(containsString("Cannot transition"));
    }

    @Test
    @Order(7)
    void shouldUpdateTaskPriority() {
        given()
                .pathParam("id", createdTaskId)
                .queryParam("priority", "URGENT")
                .when()
                .put("/api/tasks/{id}/priority")
                .then()
                .statusCode(200)
                .body("priority", equalTo("URGENT"));
    }

    @Test
    @Order(8)
    void shouldFilterTasksByStatus() {
        given()
                .queryParam("status", "IN_PROGRESS")
                .when()
                .get("/api/tasks")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("every { it.status == 'IN_PROGRESS' }", is(true));
    }

    @Test
    @Order(9)
    void shouldFilterTasksByPriority() {
        given()
                .queryParam("priority", "URGENT")
                .when()
                .get("/api/tasks")
                .then()
                .statusCode(200)
                .body("every { it.priority == 'URGENT' }", is(true));
    }

    @Test
    @Order(10)
    void shouldGetTasksDueSoon() {
        TaskCreateRequest overdueTask = new TaskCreateRequest();
        overdueTask.setTitle("Overdue Task");
        overdueTask.setDueDate(LocalDate.now().plusDays(1));

        given()
                .contentType(ContentType.JSON)
                .body(overdueTask)
                .when()
                .post("/api/tasks")
                .then()
                .statusCode(201);

        given()
                .queryParam("days", 7)
                .when()
                .get("/api/tasks/due-soon")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    @Order(11)
    void shouldGetStatistics() {
        given()
                .when()
                .get("/api/tasks/statistics")
                .then()
                .statusCode(200)
                .body("total", greaterThan(0))
                .body("completionRate", notNullValue());
    }

    @Test
    @Order(12)
    void shouldDeleteTask() {
        // Создаем задачку для удаления
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Task to delete");

        Integer taskId = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/tasks")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Удаляем
        given()
                .pathParam("id", taskId)
                .when()
                .delete("/api/tasks/{id}")
                .then()
                .statusCode(204);

        // Проверяем что задачу удалили
        given()
                .pathParam("id", taskId)
                .when()
                .get("/api/tasks/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(13)
    void shouldCreateTaskWithAssignee() {
        UserCreateDto user = new UserCreateDto();
        user.setName("Test User");
        user.setEmail("test@example.com");

        createdUserId = given()
                .contentType(ContentType.JSON)
                .body(user)
                .when()
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Task with assignee");
        request.setAssigneeId((long) createdUserId);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/tasks")
                .then()
                .statusCode(201)
                .body("assignee.id", equalTo(createdUserId))
                .body("assignee.name", equalTo("Test User"));
    }

    @Test
    @Order(14)
    @DisplayName("Should reassign task to another user")
    void shouldReassignTask() {
        User user2 = new User();
        user2.name = "Another User";
        user2.email = "another@example.com";

        int userId2 = given()
                .contentType(ContentType.JSON)
                .body(user2)
                .when()
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        given()
                .pathParam("id", createdTaskId)
                .queryParam("userId", userId2)
                .when()
                .put("/api/tasks/{id}/assign")
                .then()
                .statusCode(200)
                .body("assignee.id", equalTo(userId2));
    }

    @Test
    @Order(15)
    void shouldRejectBlankTitle() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle(""); // Пустой title

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/tasks")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(16)
    void shouldRejectPastDueDate() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Task with past due date");
        request.setDueDate(LocalDate.now().minusDays(1));

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/tasks")
                .then()
                .statusCode(500)
                .body(containsString("Due date cannot be in the past"));
    }

    @Test
    @Order(17)
    @Transactional
    void shouldHandleMultipleOperationsInTransaction() {
        for (int i = 0; i < 3; i++) {
            TaskCreateRequest request = new TaskCreateRequest();
            request.setTitle("Batch Task " + i);

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/api/tasks")
                    .then()
                    .statusCode(201);
        }

        given()
                .when()
                .get("/api/tasks")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(3));
    }
}
