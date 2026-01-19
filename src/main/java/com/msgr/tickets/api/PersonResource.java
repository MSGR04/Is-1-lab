package com.msgr.tickets.api;

import com.msgr.tickets.network.dto.PageDto;
import com.msgr.tickets.network.dto.PersonDto;
import com.msgr.tickets.network.dto.PersonUpsertDto;
import com.msgr.tickets.service.PersonService;
import com.msgr.tickets.domain.enums.Color;
import com.msgr.tickets.domain.enums.Country;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/persons")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PersonResource {

    @Inject
    private PersonService service;

    public PersonResource() {
    }

    @GET
    public PageDto<PersonDto> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("sort") @DefaultValue("id") String sort,
            @QueryParam("order") @DefaultValue("asc") String order,
            @QueryParam("id") Long id,
            @QueryParam("eyeColor") Color eyeColor,
            @QueryParam("hairColor") Color hairColor,
            @QueryParam("passportID") String passportID,
            @QueryParam("nationality") Country nationality,
            @QueryParam("locationX") Float locationX,
            @QueryParam("locationY") Float locationY,
            @QueryParam("locationZ") Float locationZ
    ) {
        if (page < 0) page = 0;
        if (size < 1) size = 1;
        if (size > 200) size = 200;
        return service.list(page, size, sort, order, id, eyeColor, hairColor, passportID, nationality, locationX, locationY, locationZ);
    }

    @GET
    @Path("/{id}")
    public PersonDto get(@PathParam("id") long id) {
        return service.get(id);
    }

    @POST
    public Response create(@Valid PersonUpsertDto body) {
        PersonDto created = service.create(body);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public PersonDto update(@PathParam("id") long id, @Valid PersonUpsertDto body) {
        return service.update(id, body);
    }

    @POST
    @Path("/{id}/rebind-delete")
    public Response rebindDelete(
            @PathParam("id") long id,
            @QueryParam("newPersonId") long newPersonId
    ) {
        if (newPersonId <= 0) throw new BadRequestException("newPersonId must be > 0");
        service.rebindTicketsAndDelete(id, newPersonId);
        return Response.noContent().build();
    }



    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
