package com.msgr.tickets.network.mapper;

import com.msgr.tickets.domain.entity.Event;
import com.msgr.tickets.network.dto.EventDto;
import com.msgr.tickets.network.dto.EventUpsertDto;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventMapper {

    public void applyUpsert(Event e, EventUpsertDto in) {
        if (e == null || in == null) return;
        e.setName(in.getName());
        e.setTicketsCount(in.getTicketsCount());
        e.setEventType(in.getEventType());
    }

    public EventDto toDto(Event e) {
        if (e == null) return null;
        EventDto dto = new EventDto();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setTicketsCount(e.getTicketsCount());
        dto.setEventType(e.getEventType());
        return dto;
    }
}
