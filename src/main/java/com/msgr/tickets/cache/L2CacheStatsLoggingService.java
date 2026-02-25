package com.msgr.tickets.cache;

import com.msgr.tickets.config.AppConfigService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class L2CacheStatsLoggingService {

    @Inject
    private AppConfigService config;

    @PersistenceUnit(unitName = "ticketsPU")
    private EntityManagerFactory entityManagerFactory;

    private final AtomicBoolean enabled = new AtomicBoolean(false);

    @PostConstruct
    void initDefaults() {
        enabled.set(config.getBoolean("app.cache.l2.stats.logging.enabled", false));
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean value) {
        enabled.set(value);
    }

    public CacheStatsSnapshot snapshot() {
        try {
            Class<?> sessionFactoryClass = Class.forName("org.hibernate.SessionFactory");
            Object sessionFactory = entityManagerFactory.unwrap(sessionFactoryClass);
            Method getStatistics = sessionFactoryClass.getMethod("getStatistics");
            Object statistics = getStatistics.invoke(sessionFactory);

            long hits = invokeStat(statistics, "getSecondLevelCacheHitCount");
            long misses = invokeStat(statistics, "getSecondLevelCacheMissCount");
            long puts = invokeStat(statistics, "getSecondLevelCachePutCount");
            return new CacheStatsSnapshot(hits, misses, puts);
        } catch (Exception e) {
            return new CacheStatsSnapshot(0, 0, 0);
        }
    }

    private long invokeStat(Object target, String methodName) {
        try {
            Object raw = target.getClass().getMethod(methodName).invoke(target);
            return raw instanceof Number ? ((Number) raw).longValue() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    public record CacheStatsSnapshot(long hitCount, long missCount, long putCount) {
        public CacheStatsSnapshot diff(CacheStatsSnapshot before) {
            if (before == null) {
                return this;
            }
            return new CacheStatsSnapshot(
                    hitCount - before.hitCount(),
                    missCount - before.missCount(),
                    putCount - before.putCount()
            );
        }
    }
}
