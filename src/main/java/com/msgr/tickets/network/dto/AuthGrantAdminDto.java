package com.msgr.tickets.network.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthGrantAdminDto {

    @NotBlank
    @Size(min = 3, max = 64)
    private String username;
}
