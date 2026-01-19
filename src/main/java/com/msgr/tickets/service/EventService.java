package com.msgr.tickets.service;
import com.msgr.tickets.network.ws.EventWsEndpoint;
import com.msgr.tickets.network.ws.EventWsMessage;

import com.msgr.tickets.network.ws.TicketWsEndpoint;
import com.msgr.tickets.network.ws.TicketWsMessage;
import com.msgr.tickets.network.dto.EventDto;
import com.msgr.tickets.network.dto.EventUpsertDto;
import com.msgr.tickets.network.dto.PageDto;
import com.msgr.tickets.domain.entity.Event;
import com.msgr.tickets.domain.enums.EventType;
import com.msgr.tickets.network.mapper.EventMapper;
import com.msgr.tickets.persistence.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.BadRequestException;

import java.util.List;

@ApplicationScoped
public class EventService {

    @Inject
    private EventRepository repo;

    @Inject
    private TicketRepository ticketRepo;

    @Inject
    private EventMapper mapper;

    public EventDto get(long id) {
        Event e = repo.findById(id).orElseThrow(() -> new NotFoundException("event not found: " + id));
        return mapper.toDto(e);
    }

    public PageDto<EventDto> list(int page, int size, String sort, String order, Long id, String name, Integer ticketsCount, EventType eventType) {
        long total = repo.count(id, name, ticketsCount, eventType);
        List<EventDto> items = repo.findPage(page, size, sort, order, id, name, ticketsCount, eventType).stream().map(mapper::toDto).toList();
        return new PageDto<>(items, total, page, size);
    }

    @Transactional
    public EventDto create(EventUpsertDto in) {
        Event e = new Event();
        mapper.applyUpsert(e, in);
        repo.save(e);

        EventWsEndpoint.broadcast(new EventWsMessage("CREATED", e.getId()));
        return mapper.toDto(e);
    }

    @Transactional
    public EventDto update(long id, EventUpsertDto in) {
        Event e = repo.findById(id).orElseThrow(() -> new NotFoundException("event not found: " + id));
        mapper.applyUpsert(e, in);
        e = repo.save(e);
        EventWsEndpoint.broadcast(new EventWsMessage("UPDATED", e.getId()));
        return mapper.toDto(e);
    }

    @Transactional
    public void delete(long id) {
        Event e = repo.findById(id).orElseThrow(() -> new NotFoundException("event not found: " + id));
        repo.delete(e);
        EventWsEndpoint.broadcast(new EventWsMessage("DELETED", id));
    }

    public long countTicketRefs(long eventId) {
        repo.findById(eventId).orElseThrow(() -> new NotFoundException("event not found: " + eventId));
        return ticketRepo.countByEventId(eventId);
    }

    @Transactional
    public void rebindTicketsAndDelete(long eventId, long newEventId) {
        if (eventId == newEventId) {
            throw new BadRequestException("newEventId must be different from eventId");
        }

        Event from = repo.findById(eventId).orElseThrow(() -> new NotFoundException("event not found: " + eventId));
        repo.findById(newEventId).orElseThrow(() -> new NotFoundException("event not found: " + newEventId));

        int updated = ticketRepo.rebindEvent(eventId, newEventId);

        if (updated > 0) {
            TicketWsEndpoint.broadcast(new TicketWsMessage("BULK_UPDATED", null));
        }

        repo.delete(from);

        EventWsEndpoint.broadcast(new EventWsMessage("DELETED", eventId));
    }


}
