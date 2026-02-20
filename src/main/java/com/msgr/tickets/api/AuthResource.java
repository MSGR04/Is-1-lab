package com.msgr.tickets.api;

import com.msgr.tickets.network.dto.AuthLoginDto;
import com.msgr.tickets.network.dto.AuthBootstrapAdminResultDto;
import com.msgr.tickets.network.dto.AuthGrantAdminDto;
import com.msgr.tickets.network.dto.AuthRegisterDto;
import com.msgr.tickets.network.dto.AuthUserDto;
import com.msgr.tickets.service.AuthService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@RequestScoped
@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    public static final String AUTH_COOKIE = "AUTH_TOKEN";
    private static final int COOKIE_AGE_SEC = 24 * 60 * 60;

    @Inject
    private AuthService authService;

    @POST
    @Path("/register")
    public Response register(@Valid AuthRegisterDto body) {
        AuthUserDto user = authService.register(body.getUsername(), body.getPassword());
        return Response.status(Response.Status.CREATED).entity(user).build();
    }

    @POST
    @Path("/login")
    public Response login(@Valid AuthLoginDto body) {
        String token = authService.login(body.getUsername(), body.getPassword());
        AuthUserDto user = authService.resolveUser(token)
                .orElseThrow(() -> new InternalServerErrorException("session creation failed"));

        return Response.ok(user)
                .cookie(buildAuthCookie(token))
                .build();
    }

    @POST
    @Path("/logout")
    public Response logout(@CookieParam(AUTH_COOKIE) String token) {
        authService.logout(token);
        return Response.ok(Map.of("ok", true))
                .cookie(clearAuthCookie())
                .build();
    }

    @GET
    @Path("/me")
    public AuthUserDto me(@CookieParam(AUTH_COOKIE) String token) {
        return authService.resolveUser(token)
                .orElseThrow(() -> new NotAuthorizedException("unauthorized"));
    }

    @POST
    @Path("/admin/grant")
    public AuthUserDto grantAdmin(
            @CookieParam(AUTH_COOKIE) String token,
            @Valid AuthGrantAdminDto body
    ) {
        return authService.grantAdmin(token, body.getUsername());
    }

    @POST
    @Path("/admin/bootstrap")
    public AuthBootstrapAdminResultDto bootstrapAdmin(
            @CookieParam(AUTH_COOKIE) String token
    ) {
        return authService.bootstrapAdmin(token);
    }

    public static NewCookie buildAuthCookie(String token) {
        return new NewCookie.Builder(AUTH_COOKIE)
                .value(token)
                .path("/")
                .httpOnly(true)
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge(COOKIE_AGE_SEC)
                .build();
    }

    public static NewCookie clearAuthCookie() {
        return new NewCookie.Builder(AUTH_COOKIE)
                .value("")
                .path("/")
                .httpOnly(true)
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge(0)
                .build();
    }
}
