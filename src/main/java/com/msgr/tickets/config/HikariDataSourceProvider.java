package com.msgr.tickets.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.util.Properties;
import javax.sql.DataSource;

public final class HikariDataSourceProvider {

    private static volatile HikariDataSource dataSource;
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in != null) {
                PROPERTIES.load(in);
            }
        } catch (Exception ignored) {
        }
    }

    private HikariDataSourceProvider() {
    }

    public static boolean isEnabled() {
        return getBoolean("app.hikari.enabled", false);
    }

    public static DataSource get() {
        HikariDataSource current = dataSource;
        if (current != null) {
            return current;
        }

        synchronized (HikariDataSourceProvider.class) {
            current = dataSource;
            if (current != null) {
                return current;
            }
            dataSource = buildDataSource();
            return dataSource;
        }
    }

    public static void close() {
        HikariDataSource current = dataSource;
        if (current != null) {
            current.close();
            dataSource = null;
        }
    }

    private static HikariDataSource buildDataSource() {
        String jdbcUrl = getString("app.hikari.jdbc-url", null);
        String username = getString("app.hikari.username", null);
        String password = getString("app.hikari.password", null);

        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalStateException("app.hikari.jdbc-url is required when HikariCP is enabled");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        if (username != null) {
            config.setUsername(username);
        }
        if (password != null) {
            config.setPassword(password);
        }
        config.setMaximumPoolSize(getInt("app.hikari.maximum-pool-size", 16));
        config.setMinimumIdle(getInt("app.hikari.minimum-idle", 4));
        config.setConnectionTimeout(getLong("app.hikari.connection-timeout-ms", 30_000L));
        config.setIdleTimeout(getLong("app.hikari.idle-timeout-ms", 600_000L));
        config.setMaxLifetime(getLong("app.hikari.max-lifetime-ms", 1_800_000L));
        config.setPoolName(getString("app.hikari.pool-name", "tickets-hikari-pool"));
        return new HikariDataSource(config);
    }

    private static String getString(String key, String defaultValue) {
        String fromSystem = System.getProperty(key);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem.trim();
        }

        String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromFile = PROPERTIES.getProperty(key);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        return defaultValue;
    }

    private static boolean getBoolean(String key, boolean defaultValue) {
        String raw = getString(key, null);
        if (raw == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(raw) || "1".equals(raw) || "yes".equalsIgnoreCase(raw);
    }

    private static int getInt(String key, int defaultValue) {
        String raw = getString(key, null);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static long getLong(String key, long defaultValue) {
        String raw = getString(key, null);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
