package com.msgr.tickets.api;

import com.msgr.tickets.network.dto.PageDto;
import com.msgr.tickets.network.dto.VenueDto;
import com.msgr.tickets.network.dto.VenueUpsertDto;
import com.msgr.tickets.service.VenueService;
import com.msgr.tickets.domain.enums.VenueType;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/venues")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class VenueResource {

    @Inject
    private VenueService service;

    public VenueResource() {
    }

    @GET
    public PageDto<VenueDto> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("sort") @DefaultValue("id") String sort,
            @QueryParam("order") @DefaultValue("asc") String order,
            @QueryParam("id") Long id,
            @QueryParam("name") String name,
            @QueryParam("type") VenueType type,
            @QueryParam("zipCode") String zipCode,
            @QueryParam("capacity") Integer capacity,
            @QueryParam("townX") Float townX,
            @QueryParam("townY") Float townY,
            @QueryParam("townZ") Float townZ
    ) {
        if (page < 0) page = 0;
        if (size < 1) size = 1;
        if (size > 200) size = 200;
        return service.list(page, size, sort, order, id, name, type, zipCode, capacity, townX, townY, townZ);
    }

    @GET
    @Path("/{id}")
    public VenueDto get(@PathParam("id") long id) {
        return service.get(id);
    }

    @POST
    public Response create(@Valid VenueUpsertDto body) {
        VenueDto created = service.create(body);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public VenueDto update(@PathParam("id") long id, @Valid VenueUpsertDto body) {
        return service.update(id, body);
    }

    @POST
    @Path("/{id}/rebind-delete")
    public Response rebindDelete(
            @PathParam("id") long id,
            @QueryParam("newVenueId") long newVenueId
    ) {
        if (newVenueId <= 0) throw new BadRequestException("newVenueId must be > 0");
        service.rebindTicketsAndDelete(id, newVenueId);
        return Response.noContent().build();
    }


    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
