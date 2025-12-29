package com.msgr.tickets.network.dto;

import com.msgr.tickets.domain.embeddable.Address;
import com.msgr.tickets.domain.enums.VenueType;
import lombok.Data;

@Data
public class VenueDto {
    private Long id;
    private String name;
    private int capacity;
    private VenueType type;
    private Address address;
}
