package com.msgr.tickets.network.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/persons")
public class PersonWsEndpoint {
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @OnMessage
    public void onMessage(Session session, String msg) {
        if ("ping".equalsIgnoreCase(msg)) session.getAsyncRemote().sendText("pong");
    }

    @OnOpen
    public void onOpen(Session session) { sessions.add(session); }

    @OnClose
    public void onClose(Session session, CloseReason reason) { sessions.remove(session); }

    @OnError
    public void onError(Session session, Throwable t) { if (session != null) sessions.remove(session); }

    public static void broadcast(PersonWsMessage msg) {
        try {
            String json = MAPPER.writeValueAsString(msg);
            for (Session s : sessions) {
                if (!s.isOpen()) { sessions.remove(s); continue; }
                try { s.getAsyncRemote().sendText(json); } catch (Exception e) { sessions.remove(s); }
            }
        } catch (Exception ignored) {}
    }
}

