package com.msgr.tickets.network.dto;

import com.msgr.tickets.domain.embeddable.Location;
import com.msgr.tickets.domain.enums.Color;
import com.msgr.tickets.domain.enums.Country;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PersonUpsertDto {

    private Color eyeColor;
    private Color hairColor;

    @NotNull
    @Valid
    private Location location;

    @NotNull
    @Size(max = 35)
    private String passportID;

    @NotNull
    private Country nationality;
}
