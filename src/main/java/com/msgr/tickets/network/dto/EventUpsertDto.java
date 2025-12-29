package com.msgr.tickets.network.dto;

import com.msgr.tickets.domain.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class EventUpsertDto {

    @NotBlank
    private String name;

    @Positive
    private Integer ticketsCount; // nullable допускается, просто не ставьте значение

    @NotNull
    private EventType eventType;
}
