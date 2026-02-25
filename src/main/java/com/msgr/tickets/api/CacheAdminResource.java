package com.msgr.tickets.api;

import com.msgr.tickets.cache.L2CacheStatsLoggingService;
import com.msgr.tickets.network.dto.AuthUserDto;
import com.msgr.tickets.network.dto.L2CacheLoggingStateDto;
import com.msgr.tickets.network.dto.L2CacheLoggingToggleDto;
import com.msgr.tickets.service.AuthService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/cache/l2")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CacheAdminResource {

    @Inject
    private AuthService authService;

    @Inject
    private L2CacheStatsLoggingService loggingService;

    @GET
    @Path("/stats-logging")
    public L2CacheLoggingStateDto getState(
            @CookieParam(AuthResource.AUTH_COOKIE) String token
    ) {
        requireAdmin(token);
        L2CacheStatsLoggingService.CacheStatsSnapshot snapshot = loggingService.snapshot();
        return new L2CacheLoggingStateDto(
                loggingService.isEnabled(),
                snapshot.hitCount(),
                snapshot.missCount(),
                snapshot.putCount()
        );
    }

    @PUT
    @Path("/stats-logging")
    public L2CacheLoggingStateDto setState(
            @CookieParam(AuthResource.AUTH_COOKIE) String token,
            L2CacheLoggingToggleDto body
    ) {
        requireAdmin(token);
        if (body == null || body.getEnabled() == null) {
            throw new BadRequestException("'enabled' must be provided");
        }
        loggingService.setEnabled(body.getEnabled());
        L2CacheStatsLoggingService.CacheStatsSnapshot snapshot = loggingService.snapshot();
        return new L2CacheLoggingStateDto(
                loggingService.isEnabled(),
                snapshot.hitCount(),
                snapshot.missCount(),
                snapshot.putCount()
        );
    }

    private void requireAdmin(String token) {
        AuthUserDto user = authService.resolveUser(token)
                .orElseThrow(() -> new NotAuthorizedException("unauthorized"));
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new jakarta.ws.rs.ForbiddenException("admin role required");
        }
    }
}
