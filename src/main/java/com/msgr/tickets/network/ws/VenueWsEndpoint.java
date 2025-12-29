package com.msgr.tickets.network.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/venues")
@ApplicationScoped
public class VenueWsEndpoint {

    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        System.out.println("[ws-venues] open " + session.getId() + " total=" + sessions.size());
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        sessions.remove(session);
        System.out.println("[ws-venues] close " + session.getId() + " reason=" + reason);
    }

    @OnError
    public void onError(Session session, Throwable t) {
        sessions.remove(session);
        System.out.println("[ws-venues] error session=" + (session == null ? "null" : session.getId()));
    }

    public static void broadcast(VenueWsMessage msg) {
        try {
            String json = MAPPER.writeValueAsString(msg);
            for (Session s : sessions) {
                if (s.isOpen()) s.getAsyncRemote().sendText(json);
            }
        } catch (Exception ignored) {}
    }
}
