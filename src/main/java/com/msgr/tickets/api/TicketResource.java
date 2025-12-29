package com.msgr.tickets.api;

import com.msgr.tickets.network.dto.PageDto;
import com.msgr.tickets.network.dto.TicketDto;
import com.msgr.tickets.network.dto.TicketUpsertDto;
import com.msgr.tickets.domain.enums.TicketType;
import com.msgr.tickets.service.TicketService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/tickets")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class TicketResource {

    @Inject
    private TicketService service;

    @GET
    public PageDto<TicketDto> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("sort") @DefaultValue("id") String sort,
            @QueryParam("order") @DefaultValue("asc") String order,
            @QueryParam("name") String name,
            @QueryParam("priceMin") Double priceMin,
            @QueryParam("priceMax") Double priceMax,
            @QueryParam("type") TicketType type,
            @QueryParam("refundable") Boolean refundable
    ) {
        if (page < 0) page = 0;
        if (size < 1) size = 1;
        if (size > 200) size = 200;

        return service.list(page, size, sort, order, name, priceMin, priceMax, type, refundable);
    }

    @GET
    @Path("/{id}")
    public TicketDto get(@PathParam("id") long id) {
        return service.get(id);
    }

    @POST
    public Response create(@Valid TicketUpsertDto body) {
        TicketDto created = service.create(body);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public TicketDto update(@PathParam("id") long id, @Valid TicketUpsertDto body) {
        return service.update(id, body);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
