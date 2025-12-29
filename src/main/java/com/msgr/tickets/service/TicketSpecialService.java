package com.msgr.tickets.service;
import com.msgr.tickets.network.ws.TicketWsEndpoint;
import com.msgr.tickets.network.ws.TicketWsMessage;

import com.msgr.tickets.network.dto.TicketDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;

@ApplicationScoped
public class TicketSpecialService {

    @PersistenceContext(unitName = "ticketsPU")
    private EntityManager em;

    @Inject
    private TicketService ticketService;

    public TicketDto minVenueTicket() {
        Object raw = em.createNativeQuery("select fn_ticket_min_venue()").getSingleResult();
        if (raw == null) throw new NotFoundException("no ticket with non-null venue exists");

        long ticketId = ((Number) raw).longValue();
        return ticketService.get(ticketId);
    }

    public List<TicketDto> ticketsWithVenueLessThan(long venueId) {
        @SuppressWarnings("unchecked")
        List<Number> ids = em.createNativeQuery(
                "select ticket_id from fn_ticket_ids_with_venue_less_than(?1)"
        ).setParameter(1, venueId).getResultList();

        return ids.stream()
                .map(Number::longValue)
                .map(ticketService::get)
                .toList();
    }

    public List<Long> uniqueNumbers() {
        @SuppressWarnings("unchecked")
        List<Number> nums = em.createNativeQuery(
                "select num from fn_ticket_unique_numbers()"
        ).getResultList();

        return nums.stream().map(Number::longValue).toList();
    }

    @Transactional
    public TicketDto cloneWithDiscountRaise(long ticketId, long percent) {
        Object raw = em.createNativeQuery("select fn_ticket_clone_with_discount_raise(?1, ?2)")
                .setParameter(1, ticketId)
                .setParameter(2, percent)
                .getSingleResult();

        long newId = ((Number) raw).longValue();

        TicketWsEndpoint.broadcast(new TicketWsMessage("CREATED", newId));

        return ticketService.get(newId);
    }

    @Transactional
    public long cancelPersonBookings(long personId) {
        Object raw = em.createNativeQuery("select fn_cancel_person_bookings(?1)")
                .setParameter(1, personId)
                .getSingleResult();

        long affected = ((Number) raw).longValue();

        if (affected > 0) {
            TicketWsEndpoint.broadcast(new TicketWsMessage("REFRESH", null));
        }
        return affected;
    }
}
