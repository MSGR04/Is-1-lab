package com.msgr.tickets.service;
import com.msgr.tickets.network.ws.PersonWsEndpoint;
import com.msgr.tickets.network.ws.PersonWsMessage;
import com.msgr.tickets.network.dto.PageDto;
import com.msgr.tickets.network.dto.PersonDto;
import com.msgr.tickets.network.dto.PersonUpsertDto;
import com.msgr.tickets.domain.entity.Person;
import com.msgr.tickets.domain.enums.Color;
import com.msgr.tickets.domain.enums.Country;
import com.msgr.tickets.network.mapper.PersonMapper;
import com.msgr.tickets.persistence.PersonRepository;
import com.msgr.tickets.network.ws.TicketWsEndpoint;
import com.msgr.tickets.network.ws.TicketWsMessage;
import com.msgr.tickets.persistence.TicketRepository;



import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;

@ApplicationScoped
public class PersonService {

    @Inject
    private PersonRepository repo;

    @Inject
    private TicketRepository ticketRepo;

    @Inject
    private PersonMapper mapper;

    public PersonDto get(long id) {
        Person p = repo.findById(id).orElseThrow(() -> new NotFoundException("person not found: " + id));
        return mapper.toDto(p);
    }

    public PageDto<PersonDto> list(
            int page, int size, String sort, String order,
            Long id, Color eyeColor, Color hairColor, String passportID, Country nationality,
            Float locationX, Float locationY, Float locationZ
    ) {
        long total = repo.count(id, eyeColor, hairColor, passportID, nationality, locationX, locationY, locationZ);
        List<PersonDto> items = repo.findPage(
                page, size, sort, order,
                id, eyeColor, hairColor, passportID, nationality, locationX, locationY, locationZ
        ).stream().map(mapper::toDto).toList();
        return new PageDto<>(items, total, page, size);
    }

    @Transactional
    public PersonDto create(PersonUpsertDto in) {
        Person p = new Person();
        mapper.applyUpsert(p, in);
        repo.save(p);
        PersonWsEndpoint.broadcast(new PersonWsMessage("CREATED", p.getId()));
        return mapper.toDto(p);
    }

    @Transactional
    public PersonDto update(long id, PersonUpsertDto in) {
        Person p = repo.findById(id).orElseThrow(() -> new NotFoundException("person not found: " + id));
        mapper.applyUpsert(p, in);
        p = repo.save(p);
        PersonWsEndpoint.broadcast(new PersonWsMessage("UPDATED", p.getId()));
        return mapper.toDto(p);
    }

    @Transactional
    public void delete(long id) {
        Person p = repo.findById(id).orElseThrow(() -> new NotFoundException("person not found: " + id));
        repo.delete(p);
        PersonWsEndpoint.broadcast(new PersonWsMessage("DELETED", id));
    }

    @Transactional
    public void rebindTicketsAndDelete(long oldPersonId, long newPersonId) {
        Person oldP = repo.findById(oldPersonId).orElseThrow(() -> new NotFoundException("person not found: " + oldPersonId));
        repo.findById(newPersonId).orElseThrow(() -> new NotFoundException("person not found: " + newPersonId));

        ticketRepo.rebindPerson(oldPersonId, newPersonId);

        TicketWsEndpoint.broadcast(new TicketWsMessage("REFRESH", null));
        repo.delete(oldP);

        PersonWsEndpoint.broadcast(new PersonWsMessage("DELETED", oldPersonId));
    }



}
