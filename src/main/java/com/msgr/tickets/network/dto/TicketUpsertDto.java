package com.msgr.tickets.network.dto;

import com.msgr.tickets.domain.enums.TicketType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.Data;

@Data
public class TicketUpsertDto {

    @NotBlank
    private String name;

    @NotNull
    @Valid
    private CoordinatesDto coordinates;

    private Long personId;

    @NotNull
    private Long eventId;

    private Long venueId;

    @Positive
    private double price;

    @NotNull
    private TicketType type;

    @Min(1)
    @Max(100)
    private long discount;

    @Positive
    private long number;

    @NotBlank
    private String comment;

    private boolean refundable;
}
