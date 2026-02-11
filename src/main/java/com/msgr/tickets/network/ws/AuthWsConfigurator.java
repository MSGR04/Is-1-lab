package com.msgr.tickets.network.ws;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

import java.util.List;

public class AuthWsConfigurator extends ServerEndpointConfig.Configurator {

    public static final String COOKIE_HEADER_KEY = "cookieHeader";

    @Override
    public void modifyHandshake(
            ServerEndpointConfig sec,
            HandshakeRequest request,
            HandshakeResponse response
    ) {
        List<String> cookieHeaders = request.getHeaders().get("cookie");
        if (cookieHeaders != null && !cookieHeaders.isEmpty()) {
            sec.getUserProperties().put(COOKIE_HEADER_KEY, cookieHeaders.get(0));
        }
        super.modifyHandshake(sec, request, response);
    }
}
