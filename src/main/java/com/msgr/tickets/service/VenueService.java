package com.msgr.tickets.service;

import com.msgr.tickets.network.ws.TicketWsEndpoint;
import com.msgr.tickets.network.ws.TicketWsMessage;
import com.msgr.tickets.network.ws.VenueWsEndpoint;
import com.msgr.tickets.network.ws.VenueWsMessage;
import com.msgr.tickets.network.dto.PageDto;
import com.msgr.tickets.network.dto.VenueDto;
import com.msgr.tickets.network.dto.VenueUpsertDto;
import com.msgr.tickets.domain.entity.Venue;
import com.msgr.tickets.domain.enums.VenueType;
import com.msgr.tickets.network.mapper.VenueMapper;
import com.msgr.tickets.persistence.TicketRepository;
import com.msgr.tickets.persistence.VenueRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;

@ApplicationScoped
public class VenueService {

    @Inject
    private VenueRepository repo;

    @Inject private TicketRepository ticketRepo;

    @Inject
    private VenueMapper mapper;

    public VenueDto get(long id) {
        Venue v = repo.findById(id).orElseThrow(() -> new NotFoundException("venue not found: " + id));
        return mapper.toDto(v);
    }

    public PageDto<VenueDto> list(
            int page, int size, String sort, String order,
            Long id, String name, VenueType type, String zipCode, Integer capacity,
            Float townX, Float townY, Float townZ
    ) {
        long total = repo.count(id, name, type, zipCode, capacity, townX, townY, townZ);
        List<VenueDto> items = repo.findPage(page, size, sort, order, id, name, type, zipCode, capacity, townX, townY, townZ)
                .stream().map(mapper::toDto).toList();
        return new PageDto<>(items, total, page, size);
    }

    @Transactional
    public VenueDto create(VenueUpsertDto in) {
        Venue v = new Venue();
        mapper.applyUpsert(v, in);
        repo.save(v);

        VenueWsEndpoint.broadcast(new VenueWsMessage("CREATED", v.getId()));
        return mapper.toDto(v);
    }

    @Transactional
    public VenueDto update(long id, VenueUpsertDto in) {
        Venue v = repo.findById(id).orElseThrow(() -> new NotFoundException("venue not found: " + id));
        mapper.applyUpsert(v, in);  
        v = repo.save(v);

        VenueWsEndpoint.broadcast(new VenueWsMessage("CREATED", v.getId()));
        return mapper.toDto(v);
    }

    @Transactional
    public void delete(long id) {
        Venue v = repo.findById(id).orElseThrow(() -> new NotFoundException("venue not found: " + id));
        repo.delete(v);

        VenueWsEndpoint.broadcast(new VenueWsMessage("DELETED", id));
    }

    @Transactional
    public void rebindTicketsAndDelete(long oldVenueId, long newVenueId) {
        Venue oldV = repo.findById(oldVenueId).orElseThrow(() -> new NotFoundException("venue not found: " + oldVenueId));
        repo.findById(newVenueId).orElseThrow(() -> new NotFoundException("venue not found: " + newVenueId));

        ticketRepo.rebindVenue(oldVenueId, newVenueId);

        TicketWsEndpoint.broadcast(new TicketWsMessage("REFRESH", null));

        repo.delete(oldV);

        VenueWsEndpoint.broadcast(new VenueWsMessage("DELETED", oldVenueId));
    }


}
