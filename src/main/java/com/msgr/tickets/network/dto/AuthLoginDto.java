package com.msgr.tickets.network.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthLoginDto {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
