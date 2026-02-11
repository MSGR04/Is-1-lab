package com.msgr.tickets.network.ws;

import com.msgr.tickets.api.AuthResource;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public final class AuthWsUtil {

    private AuthWsUtil() {
    }

    public static boolean authorizeOrClose(Session session) {
        try {
            Object raw = session.getUserProperties().get(AuthWsConfigurator.COOKIE_HEADER_KEY);
            String cookieHeader = raw == null ? null : raw.toString();
            String token = extractCookie(cookieHeader, AuthResource.AUTH_COOKIE);
            if (token == null || token.isBlank()) {
                closeUnauthorized(session);
                return false;
            }

            if (!isTokenValid(token)) {
                closeUnauthorized(session);
                return false;
            }
            return true;
        } catch (Exception e) {
            try {
                closeUnauthorized(session);
            } catch (Exception ignored) {
            }
            return false;
        }
    }

    private static void closeUnauthorized(Session session) throws Exception {
        session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthorized"));
    }

    private static boolean isTokenValid(String token) {
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:jboss/datasources/TicketsDS");

            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "select 1 from auth_sessions where token = ? and expires_at > ?"
                 )) {
                ps.setString(1, token);
                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static String extractCookie(String cookieHeader, String key) {
        if (cookieHeader == null || cookieHeader.isBlank()) return null;
        String[] parts = cookieHeader.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            int idx = trimmed.indexOf('=');
            if (idx <= 0) continue;
            String name = trimmed.substring(0, idx).trim();
            String value = trimmed.substring(idx + 1).trim();
            if (key.equals(name)) return value;
        }
        return null;
    }
}
