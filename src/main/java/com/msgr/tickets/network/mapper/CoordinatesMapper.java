package com.msgr.tickets.network.mapper;

import com.msgr.tickets.domain.embeddable.Coordinates;
import com.msgr.tickets.network.dto.CoordinatesDto;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CoordinatesMapper {

    public Coordinates toEmbeddable(CoordinatesDto dto) {
        if (dto == null) return null;

        Coordinates c = new Coordinates();
        c.setX(dto.getX());
        c.setY(dto.getY());
        return c;
    }

    public CoordinatesDto toDto(Coordinates c) {
        if (c == null) return null;

        CoordinatesDto dto = new CoordinatesDto();
        dto.setX(c.getX());
        dto.setY(c.getY());
        return dto;
    }
}
