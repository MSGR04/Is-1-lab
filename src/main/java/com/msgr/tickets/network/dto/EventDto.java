package com.msgr.tickets.network.dto;

import com.msgr.tickets.domain.enums.EventType;
import lombok.Data;

@Data
public class EventDto {
    private Long id;
    private String name;
    private Integer ticketsCount;
    private EventType eventType;
}
