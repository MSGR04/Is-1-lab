package com.msgr.tickets.network.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CoordinatesDto {

    @NotNull
    @Max(265)
    private Integer x;

    @NotNull
    private Long y;
}
