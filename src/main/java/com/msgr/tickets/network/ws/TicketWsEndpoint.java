package com.msgr.tickets.network.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/tickets")
public class TicketWsEndpoint {

    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @OnMessage
    public void onMessage(Session session, String msg) {
        // отвечаем на heartbeat
        if ("ping".equalsIgnoreCase(msg)) {
            session.getAsyncRemote().sendText("pong");
        }
    }
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        System.out.println("[ws] open " + session.getId() + " total=" + sessions.size());
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        sessions.remove(session);
        System.out.println("[ws] close " + session.getId() + " reason=" + reason);
    }

    @OnError
    public void onError(Session session, Throwable t) {
        if (session != null) sessions.remove(session);
        System.out.println("[ws] error session=" + (session != null ? session.getId() : "null"));
        t.printStackTrace();
    }

    public static void broadcast(TicketWsMessage msg) {
        try {
            String json = MAPPER.writeValueAsString(msg);
            System.out.println("[ws] broadcast " + json + " sessions=" + sessions.size());

            for (Session s : sessions) {
                if (!s.isOpen()) {
                    sessions.remove(s);
                    continue;
                }
                try {
                    s.getAsyncRemote().sendText(json);
                } catch (Exception e) {
                    sessions.remove(s);
                }
            }
        } catch (Exception e) {
            System.out.println("[ws] broadcast failed");
            e.printStackTrace();
        }
    }
}
