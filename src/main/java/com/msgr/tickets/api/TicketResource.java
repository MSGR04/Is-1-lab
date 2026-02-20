package com.msgr.tickets.api;

import com.msgr.tickets.network.dto.AuthUserDto;
import com.msgr.tickets.network.dto.PageDto;
import com.msgr.tickets.network.dto.TicketDto;
import com.msgr.tickets.network.dto.TicketImportHistoryDto;
import com.msgr.tickets.network.dto.TicketImportResultDto;
import com.msgr.tickets.network.dto.TicketUpsertDto;
import com.msgr.tickets.domain.enums.TicketType;
import com.msgr.tickets.service.AuthService;
import com.msgr.tickets.service.TicketService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Path("/tickets")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class TicketResource {

    @Inject
    private TicketService service;

    @Inject
    private AuthService authService;

    @GET
    public PageDto<TicketDto> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("sort") @DefaultValue("id") String sort,
            @QueryParam("order") @DefaultValue("asc") String order,
            @QueryParam("id") Long id,
            @QueryParam("name") String name,
            @QueryParam("comment") String comment,
            @QueryParam("coordinatesX") Integer coordinatesX,
            @QueryParam("coordinatesY") Long coordinatesY,
            @QueryParam("creationDate") String creationDate,
            @QueryParam("personId") Long personId,
            @QueryParam("eventId") Long eventId,
            @QueryParam("venueId") Long venueId,
            @QueryParam("priceMin") Double priceMin,
            @QueryParam("priceMax") Double priceMax,
            @QueryParam("price") Double price,
            @QueryParam("type") TicketType type,
            @QueryParam("discount") Long discount,
            @QueryParam("number") Long number,
            @QueryParam("refundable") Boolean refundable
    ) {
        if (page < 0) page = 0;
        if (size < 1) size = 1;
        if (size > 200) size = 200;

        return service.list(
                page, size, sort, order,
                id, name, comment, coordinatesX, coordinatesY, creationDate,
                personId, eventId, venueId,
                priceMin, priceMax, price, type, discount, number, refundable
        );
    }

    @GET
    @Path("/{id}")
    public TicketDto get(@PathParam("id") long id) {
        return service.get(id);
    }

    @POST
    public Response create(@Valid TicketUpsertDto body) {
        TicketDto created = service.create(body);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @POST
    @Path("/import/csv")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importCsv(
            @CookieParam(AuthResource.AUTH_COOKIE) String token,
            MultipartFormDataInput formData
    ) {
        AuthUserDto currentUser = currentUser(token);
        String csvBody = extractCsvBody(formData);
        try {
            TicketImportResultDto result = service.importCsv(currentUser.getId(), csvBody);
            return Response.status(Response.Status.CREATED).entity(result).build();
        } catch (BadRequestException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(formatImportBadRequestMessage(e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/import/history")
    public List<TicketImportHistoryDto> importHistory(
            @CookieParam(AuthResource.AUTH_COOKIE) String token
    ) {
        AuthUserDto currentUser = currentUser(token);
        return service.listImportHistory(currentUser.getId(), currentUser.getRole());
    }

    @PUT
    @Path("/{id}")
    public TicketDto update(@PathParam("id") long id, @Valid TicketUpsertDto body) {
        return service.update(id, body);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    private AuthUserDto currentUser(String token) {
        return authService.resolveUser(token)
                .orElseThrow(() -> new NotAuthorizedException("unauthorized"));
    }

    private boolean isUniquenessViolation(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("must be unique")
                || normalized.contains("не соблюдены условия уникальности");
    }

    private String formatImportBadRequestMessage(String message) {
        if (isUniquenessViolation(message)) {
            return "не соблюдены условия уникальности";
        }
        if (message == null || message.isBlank()) {
            return "данные файла не прошли валидацию";
        }
        return message;
    }

    private String extractCsvBody(MultipartFormDataInput formData) {
        if (formData == null) {
            throw new BadRequestException("multipart/form-data body is required");
        }

        Map<String, List<InputPart>> parts = formData.getFormDataMap();
        List<InputPart> fileParts = parts == null ? null : parts.get("file");
        if (fileParts == null || fileParts.isEmpty()) {
            throw new BadRequestException("multipart field 'file' is required");
        }

        try (InputStream inputStream = fileParts.get(0).getBody(InputStream.class, null)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BadRequestException("failed to read uploaded file");
        }
    }
}
