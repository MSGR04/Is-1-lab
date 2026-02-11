package com.msgr.tickets.service;

import com.msgr.tickets.network.ws.TicketWsEndpoint;
import com.msgr.tickets.network.ws.TicketWsMessage;
import com.msgr.tickets.network.dto.PageDto;
import com.msgr.tickets.network.dto.TicketDto;
import com.msgr.tickets.network.dto.TicketUpsertDto;
import com.msgr.tickets.domain.entity.Event;
import com.msgr.tickets.domain.entity.Person;
import com.msgr.tickets.domain.entity.Ticket;
import com.msgr.tickets.domain.entity.Venue;
import com.msgr.tickets.domain.enums.TicketType;
import com.msgr.tickets.network.mapper.CoordinatesMapper;
import com.msgr.tickets.persistence.TicketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class TicketService {

    @Inject
    private TicketRepository repo;
    @Inject
    private CoordinatesMapper coordinatesMapper;

    @Inject private PersonService personService;
    @Inject private EventService eventService;
    @Inject private VenueService venueService;

    @PersistenceContext(unitName = "ticketsPU")
    private EntityManager em;

    public TicketDto get(long id) {
        Ticket t = repo.findById(id).orElseThrow(() -> new NotFoundException("ticket not found: " + id));
        return toDto(t);
    }

    public PageDto<TicketDto> list(
            int page, int size,
            String sort, String order,
            Long id, String name, String comment, Integer coordinatesX, Long coordinatesY, String creationDate,
            Long personId, Long eventId, Long venueId,
            Double priceMin, Double priceMax, Double price, TicketType type, Long discount, Long number, Boolean refundable
    ) {
        long total = repo.count(
                id, name, comment, coordinatesX, coordinatesY, creationDate,
                personId, eventId, venueId, priceMin, priceMax, price, type, discount, number, refundable
        );
        List<TicketDto> items = repo.findPage(
                page, size, sort, order,
                id, name, comment, coordinatesX, coordinatesY, creationDate,
                personId, eventId, venueId, priceMin, priceMax, price, type, discount, number, refundable
        )
                .stream().map(this::toDto).toList();
        return new PageDto<>(items, total, page, size);
    }

    @Transactional
    public TicketDto create(TicketUpsertDto in) {
        Ticket t = new Ticket();
        applyUpsert(t, in);
        t.setCreationDate(LocalDate.now());
        repo.save(t);

        TicketWsEndpoint.broadcast(new TicketWsMessage("CREATED", t.getId()));
        return toDto(t);
    }

    @Transactional
    public TicketDto update(long id, TicketUpsertDto in) {
        Ticket t = repo.findById(id).orElseThrow(() -> new NotFoundException("ticket not found: " + id));
        applyUpsert(t, in);
        t = repo.save(t);

        TicketWsEndpoint.broadcast(new TicketWsMessage("UPDATED", t.getId()));
        return toDto(t);
    }

    @Transactional
    public void delete(long id) {
        Ticket t = repo.findById(id).orElseThrow(() -> new NotFoundException("ticket not found: " + id));
        repo.delete(t);

        TicketWsEndpoint.broadcast(new TicketWsMessage("DELETED", id));
    }

    private void applyUpsert(Ticket t, TicketUpsertDto in) {
        t.setName(in.getName());
        t.setCoordinates(coordinatesMapper.toEmbeddable(in.getCoordinates()));

        // Business rule for this lab: personId=0 means "no person".
        Long personId = in.getPersonId();
        if (personId != null && personId <= 0) {
            personId = null;
        }

        Person person = null;
        if (personId != null) {
            person = em.find(Person.class, personId);
            if (person == null) throw new NotFoundException("person not found: " + personId);
        }

        Event event = em.find(Event.class, in.getEventId());
        if (event == null) throw new NotFoundException("event not found: " + in.getEventId());

        Venue venue = null;
        if (in.getVenueId() != null) {
            venue = em.find(Venue.class, in.getVenueId());
            if (venue == null) throw new NotFoundException("venue not found: " + in.getVenueId());
        }

        t.setPerson(person);
        t.setEvent(event);
        t.setVenue(venue);

        t.setPrice(in.getPrice());
        t.setType(in.getType());
        t.setDiscount(in.getDiscount());
        t.setNumber(in.getNumber());
        t.setComment(in.getComment());
        t.setRefundable(in.isRefundable());
    }

    private TicketDto toDto(Ticket t) {
        TicketDto dto = new TicketDto();
        dto.setId(t.getId());
        dto.setName(t.getName());
        dto.setCoordinates(coordinatesMapper.toDto(t.getCoordinates()));
        dto.setCreationDate(t.getCreationDate());

        dto.setPersonId(t.getPerson() == null ? null : t.getPerson().getId());
        dto.setEventId(t.getEvent().getId());
        dto.setVenueId(t.getVenue() == null ? null : t.getVenue().getId());

        dto.setPerson(t.getPerson() == null ? null : personService.get(t.getPerson().getId()));
        dto.setEvent(eventService.get(t.getEvent().getId()));
        dto.setVenue(t.getVenue() == null ? null : venueService.get(t.getVenue().getId()));


        dto.setPrice(t.getPrice());
        dto.setType(t.getType());
        dto.setDiscount(t.getDiscount());
        dto.setNumber(t.getNumber());
        dto.setComment(t.getComment());
        dto.setRefundable(t.isRefundable());
        return dto;
    }
}
