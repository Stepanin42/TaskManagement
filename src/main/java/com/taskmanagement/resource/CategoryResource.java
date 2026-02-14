package com.taskmanagement.resource;

import com.taskmanagement.model.entity.Category;
import com.taskmanagement.service.CategoryService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@AllArgsConstructor
public class CategoryResource {
    private final CategoryService categoryService;

    @GET
    public Response getCategories() {
        return Response.ok(categoryService.findAll()).build();
    }

    @Path("/{name}")
    @GET
    public Response getCategory(@PathParam("name") String name) {
        return Response.ok(categoryService.findByName(name)).build();
    }

    @Path("/{id}")
    @GET
    public Response getCategoryById(@PathParam("id") Long id) {
        return Response.ok(categoryService.findById(id)).build();
    }

    @POST
    public Response createCategory(String name) {
        categoryService.save(name);
        return Response.ok().build();
    }

}
