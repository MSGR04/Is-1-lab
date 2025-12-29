package com.msgr.tickets.network.dto;

import com.msgr.tickets.domain.embeddable.Address;
import com.msgr.tickets.domain.enums.VenueType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class VenueUpsertDto {

    @NotBlank
    private String name;

    @Positive
    private int capacity;

    @NotNull
    private VenueType type;

    @NotNull
    @Valid
    private Address address;
}
