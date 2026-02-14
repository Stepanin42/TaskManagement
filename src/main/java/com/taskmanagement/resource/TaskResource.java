package com.taskmanagement.resource;

import com.taskmanagement.model.dto.TaskCreateRequest;
import com.taskmanagement.model.dto.TaskStatistics;
import com.taskmanagement.model.entity.Task;
import com.taskmanagement.model.enums.TaskPriority;
import com.taskmanagement.model.enums.TaskStatus;
import com.taskmanagement.service.StatisticsService;
import com.taskmanagement.service.TaskService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Path("/api/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {

    @Inject
    TaskService taskService;

    @Inject
    StatisticsService statisticsService;

    private static final Logger LOG = LoggerFactory.getLogger(TaskResource.class);

    @GET
    public Response getAllTasks(@QueryParam("status") TaskStatus status,
                                @QueryParam("priority") TaskPriority priority,
                                @QueryParam("userId") Long userId) {
        try {
            List<Task> tasks;

            if (status != null || priority != null || userId != null) {
                tasks = taskService.filterTasks(status, priority, userId);
            } else {
                tasks = taskService.getAllTasks();
            }

            return Response.ok(tasks).build();
        } catch (Exception e) {
            LOG.error("Error getting tasks", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getTaskById(@PathParam("id") Long id) {
        try {
            Task task = taskService.getTaskById(id);
            return Response.ok(task).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @POST
    public Response createTask(TaskCreateRequest request) {
        try {
            Task task = taskService.createTask(request);
            return Response.status(Response.Status.CREATED)
                    .entity(task)
                    .build();
        } catch (ValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @PUT
    @Path("/{id}/status")
    public Response updateStatus(@PathParam("id") Long id,
                                 @QueryParam("status") TaskStatus newStatus) {
        try {
            Task task = taskService.updateStatus(id, newStatus);
            return Response.ok(task).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @PUT
    @Path("/{id}/priority")
    public Response updatePriority(@PathParam("id") Long id,
                                   @QueryParam("priority") TaskPriority priority) {
        try {
            Task task = taskService.updatePriority(id, priority);
            return Response.ok(task).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @PUT
    @Path("/{id}/assign")
    public Response assignTask(@PathParam("id") Long taskId,
                               @QueryParam("userId") Long userId) {
        try {
            Task task = taskService.assignTask(taskId, userId);
            return Response.ok(task).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteTask(@PathParam("id") Long id) {
        try {
            taskService.deleteTask(id);
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/overdue")
    public Response getOverdueTasks() {
        List<Task> tasks = taskService.getOverdueTasks();
        return Response.ok(tasks).build();
    }

    @GET
    @Path("/due-soon")
    public Response getTasksDueSoon(@QueryParam("days") @DefaultValue("7") int days) {
        List<Task> tasks = taskService.getTasksDueSoon(days);
        return Response.ok(tasks).build();
    }

    @GET
    @Path("/statistics")
    public Response getStatistics() {
        TaskStatistics stats = statisticsService.getTaskStatistics();
        return Response.ok(stats).build();
    }
}
