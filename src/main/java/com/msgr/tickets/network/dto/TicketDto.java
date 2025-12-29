package com.msgr.tickets.network.dto;

import com.msgr.tickets.domain.enums.TicketType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TicketDto {
    private Long id;
    private String name;
    private CoordinatesDto coordinates;
    private LocalDate creationDate;

    private Long personId;
    private Long eventId;
    private Long venueId;

    private PersonDto person;
    private EventDto event;
    private VenueDto venue;

    private double price;
    private TicketType type;
    private long discount;
    private long number;
    private String comment;
    private boolean refundable;
}
