package com.msgr.tickets.network.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketImportHistoryDto {
    private Long id;
    private String status;
    private String username;
    private Integer importedCount;
}
