package com.msgr.tickets.cache;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.logging.Logger;

@LogL2CacheStats
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 100)
public class L2CacheStatsLoggingInterceptor {

    private static final Logger LOG = Logger.getLogger(L2CacheStatsLoggingInterceptor.class.getName());

    @Inject
    private L2CacheStatsLoggingService loggingService;

    @AroundInvoke
    public Object around(InvocationContext ctx) throws Exception {
        if (!loggingService.isEnabled()) {
            return ctx.proceed();
        }

        L2CacheStatsLoggingService.CacheStatsSnapshot before = loggingService.snapshot();
        try {
            return ctx.proceed();
        } finally {
            L2CacheStatsLoggingService.CacheStatsSnapshot after = loggingService.snapshot();
            L2CacheStatsLoggingService.CacheStatsSnapshot diff = after.diff(before);
            LOG.info(
                    "L2 cache stats [" + ctx.getMethod().getDeclaringClass().getSimpleName() + "." + ctx.getMethod().getName() + "] "
                            + "hitsDelta=" + diff.hitCount()
                            + ", missesDelta=" + diff.missCount()
                            + ", putsDelta=" + diff.putCount()
                            + " | totals: hits=" + after.hitCount()
                            + ", misses=" + after.missCount()
                            + ", puts=" + after.putCount()
            );
        }
    }
}
