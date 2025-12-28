package com.taskmanagement.resource;

import com.taskmanagement.model.dto.UserStatistics;
import com.taskmanagement.model.entity.User;
import com.taskmanagement.model.repository.UserRepository;
import com.taskmanagement.service.StatisticsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserRepository userRepository;

    @Inject
    StatisticsService statisticsService;

    @GET
    public Response getAllUsers() {
        List<User> users = userRepository.findAllUser();
        return Response.ok(users).build();
    }

    @GET
    @Path("/{id}")
    public Response getUserById(@PathParam("id") Long id) {
        return Optional.of(userRepository.findById(id))
                .map(user -> Response.ok(user).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/{id}/statistics")
    public Response getUserStatistics(@PathParam("id") Long id) {
        try {
            UserStatistics stats = statisticsService.getUserStatistics(id);
            return Response.ok(stats).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @POST
    public Response createUser(User user) {
        User saved = userRepository.save(user);
        return Response.status(Response.Status.CREATED)
                .entity(saved)
                .build();
    }
}
