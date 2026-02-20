package com.msgr.tickets.network.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthBootstrapAdminResultDto {
    private boolean granted;
    private String message;
    private AuthUserDto user;
}
