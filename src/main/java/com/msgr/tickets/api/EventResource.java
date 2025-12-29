package com.msgr.tickets.api;

import com.msgr.tickets.network.dto.EventDto;
import com.msgr.tickets.network.dto.EventUpsertDto;
import com.msgr.tickets.network.dto.PageDto;
import com.msgr.tickets.service.EventService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/events")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EventResource {

    @Inject
    private EventService service;

    public EventResource() {
    }

    @GET
    public PageDto<EventDto> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("sort") @DefaultValue("id") String sort,
            @QueryParam("order") @DefaultValue("asc") String order
    ) {
        if (page < 0) page = 0;
        if (size < 1) size = 1;
        if (size > 200) size = 200;
        return service.list(page, size, sort, order);
    }

    @GET
    @Path("/{id}")
    public EventDto get(@PathParam("id") long id) {
        return service.get(id);
    }

    @POST
    public Response create(@Valid EventUpsertDto body) {
        EventDto created = service.create(body);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @GET
    @Path("/{id}/refs")
    public Response refs(@PathParam("id") long id) {
        long c = service.countTicketRefs(id);
        return Response.ok(new java.util.HashMap<String, Object>() {{
            put("tickets", c);
        }}).build();
    }

    @POST
    @Path("/{id}/rebind-delete")
    public Response rebindDelete(
            @PathParam("id") long id,
            @QueryParam("newEventId") long newEventId
    ) {
        if (newEventId <= 0) throw new BadRequestException("newEventId must be > 0");
        service.rebindTicketsAndDelete(id, newEventId);
        return Response.noContent().build();
    }



    @PUT
    @Path("/{id}")
    public EventDto update(@PathParam("id") long id, @Valid EventUpsertDto body) {
        return service.update(id, body);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
