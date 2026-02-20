package com.msgr.tickets.api;

import com.msgr.tickets.service.AuthService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    @Inject
    private AuthService authService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        if (path == null) path = "";
        path = path.trim();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        if (path.startsWith("auth/") || "auth".equals(path)) {
            return;
        }

        Cookie cookie = requestContext.getCookies().get(AuthResource.AUTH_COOKIE);
        String token = cookie == null ? null : cookie.getValue();

        boolean ok = authService.resolveUser(token).isPresent();
        if (!ok) {
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("unauthorized")
                            .build()
            );
        }
    }
}
