package com.msgr.tickets.network.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketWsMessage {
    private String type;
    private Long ticketId;
}
