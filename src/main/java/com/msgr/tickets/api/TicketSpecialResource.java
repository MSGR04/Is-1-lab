package com.msgr.tickets.api;

import com.msgr.tickets.network.dto.TicketDto;
import com.msgr.tickets.service.TicketSpecialService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@RequestScoped
@Path("/tickets/special")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TicketSpecialResource {

    @Inject
    private TicketSpecialService service;

    // 1) Ticket с минимальным venue
    @GET
    @Path("/min-venue")
    public TicketDto minVenue() {
        return service.minVenueTicket();
    }

    // 2) Tickets, где venue < заданного
    @GET
    @Path("/venue-less-than")
    public List<TicketDto> venueLessThan(@QueryParam("venueId") long venueId) {
        if (venueId <= 0) throw new BadRequestException("venueId must be > 0");
        return service.ticketsWithVenueLessThan(venueId);
    }

    // 3) Уникальные number
    @GET
    @Path("/unique-numbers")
    public List<Long> uniqueNumbers() {
        return service.uniqueNumbers();
    }

    // 4) Клонировать Ticket + discount=% и поднять price на сумму скидки
    @POST
    @Path("/{id}/clone-with-discount")
    public TicketDto cloneWithDiscount(
            @PathParam("id") long id,
            @QueryParam("percent") @Min(1) @Max(100) long percent
    ) {
        return service.cloneWithDiscountRaise(id, percent);
    }

    // 5) Отменить бронирования person
    @POST
    @Path("/cancel-person-bookings")
    public long cancelPersonBookings(@QueryParam("personId") long personId) {
        if (personId < 0) throw new BadRequestException("personId must be >= 0");
        return service.cancelPersonBookings(personId);
    }
}
