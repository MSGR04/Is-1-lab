package com.msgr.tickets.network.mapper;

import com.msgr.tickets.domain.entity.Venue;
import com.msgr.tickets.network.dto.VenueDto;
import com.msgr.tickets.network.dto.VenueUpsertDto;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VenueMapper {

    public void applyUpsert(Venue v, VenueUpsertDto in) {
        if (v == null || in == null) return;
        v.setName(in.getName());
        v.setCapacity(in.getCapacity());
        v.setType(in.getType());
        v.setAddress(in.getAddress());
    }

    public VenueDto toDto(Venue v) {
        if (v == null) return null;
        VenueDto dto = new VenueDto();
        dto.setId(v.getId());
        dto.setName(v.getName());
        dto.setCapacity(v.getCapacity());
        dto.setType(v.getType());
        dto.setAddress(v.getAddress());
        return dto;
    }
}
