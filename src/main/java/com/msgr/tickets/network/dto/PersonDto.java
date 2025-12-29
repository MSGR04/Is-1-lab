package com.msgr.tickets.network.dto;

import com.msgr.tickets.domain.embeddable.Location;
import com.msgr.tickets.domain.enums.Color;
import com.msgr.tickets.domain.enums.Country;
import lombok.Data;

@Data
public class PersonDto {
    private Long id;
    private Color eyeColor;
    private Color hairColor;
    private Location location;
    private String passportID;
    private Country nationality;
}
