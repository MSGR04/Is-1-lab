package com.msgr.tickets.network.mapper;

import com.msgr.tickets.domain.entity.Person;
import com.msgr.tickets.network.dto.PersonDto;
import com.msgr.tickets.network.dto.PersonUpsertDto;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PersonMapper {

    public void applyUpsert(Person p, PersonUpsertDto in) {
        if (p == null || in == null) return;
        p.setEyeColor(in.getEyeColor());
        p.setHairColor(in.getHairColor());
        p.setLocation(in.getLocation());
        p.setPassportID(in.getPassportID());
        p.setNationality(in.getNationality());
    }

    public PersonDto toDto(Person p) {
        if (p == null) return null;
        PersonDto dto = new PersonDto();
        dto.setId(p.getId());
        dto.setEyeColor(p.getEyeColor());
        dto.setHairColor(p.getHairColor());
        dto.setLocation(p.getLocation());
        dto.setPassportID(p.getPassportID());
        dto.setNationality(p.getNationality());
        return dto;
    }
}
