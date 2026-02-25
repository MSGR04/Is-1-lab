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
import jakarta.persistence.PersistenceException;
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
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/tickets")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class TicketResource {
    private static final Pattern FILENAME_PATTERN = Pattern.compile("filename\\*?=(?:UTF-8''|\\\")?([^\\\";]+)");

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
            @QueryParam("simulateFailure") @DefaultValue("false") boolean simulateFailure,
            @QueryParam("simulateDbFailure") @DefaultValue("false") boolean simulateDbFailure,
            MultipartFormDataInput formData
    ) {
        AuthUserDto currentUser = currentUser(token);
        UploadedCsvFile file = extractCsvFile(formData);
        try {
            TicketImportResultDto result = service.importCsv(
                    currentUser.getId(),
                    file.fileName(),
                    file.contentType(),
                    file.body(),
                    simulateFailure,
                    simulateDbFailure
            );
            return Response.status(Response.Status.CREATED).entity(result).build();
        } catch (BadRequestException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(formatImportBadRequestMessage(e.getMessage()))
                    .build();
        } catch (ServiceUnavailableException e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .type(MediaType.TEXT_PLAIN)
                    .entity("Minio не запущен")
                    .build();
        } catch (RuntimeException e) {
            if (isSerializationConflict(e)) {
                return Response.status(Response.Status.CONFLICT)
                        .type(MediaType.TEXT_PLAIN)
                        .entity("concurrent transaction conflict, retry import")
                        .build();
            }
            throw e;
        }
    }

    @POST
    @Path("/import/csv/simulate/db-down")
    public Response simulateDbDownForNextImport(
            @CookieParam(AuthResource.AUTH_COOKIE) String token
    ) {
        currentUser(token);
        service.armNextImportDbFailure();
        throw new PersistenceException("simulated DB down is armed for next import");
    }

    @POST
    @Path("/import/csv/simulate/business-error-next")
    public Response simulateBusinessErrorForNextImport(
            @CookieParam(AuthResource.AUTH_COOKIE) String token
    ) {
        currentUser(token);
        service.armNextImportBusinessFailure();
        return Response.noContent().build();
    }

    @GET
    @Path("/import/history")
    public List<TicketImportHistoryDto> importHistory(
            @CookieParam(AuthResource.AUTH_COOKIE) String token
    ) {
        AuthUserDto currentUser = currentUser(token);
        return service.listImportHistory(currentUser.getId(), currentUser.getRole());
    }

    @GET
    @Path("/import/history/{operationId}/file")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadImportFile(
            @CookieParam(AuthResource.AUTH_COOKIE) String token,
            @PathParam("operationId") long operationId
    ) {
        AuthUserDto currentUser = currentUser(token);
        TicketService.ImportFileDownload file = service.downloadImportFile(
                currentUser.getId(),
                currentUser.getRole(),
                operationId
        );
        return Response.ok(file.bytes(), file.contentType())
                .header("Content-Disposition", "attachment; filename=\"" + file.fileName() + "\"")
                .build();
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

    private boolean isSerializationConflict(Throwable error) {
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < 20) {
            if (current instanceof SQLException sqlException) {
                String state = sqlException.getSQLState();
                if ("40001".equals(state) || "40P01".equals(state)) {
                    return true;
                }
            }

            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("could not serialize access")
                        || normalized.contains("serialization failure")
                        || normalized.contains("deadlock detected")
                        || normalized.contains("lock acquisition")) {
                    return true;
                }
            }

            current = current.getCause();
            depth++;
        }
        return false;
    }
    private UploadedCsvFile extractCsvFile(MultipartFormDataInput formData) {
        if (formData == null) {
            throw new BadRequestException("multipart/form-data body is required");
        }

        Map<String, List<InputPart>> parts = formData.getFormDataMap();
        List<InputPart> fileParts = parts == null ? null : parts.get("file");
        if (fileParts == null || fileParts.isEmpty()) {
            throw new BadRequestException("multipart field 'file' is required");
        }

        InputPart filePart = fileParts.get(0);
        String fileName = extractFileName(filePart);
        String contentType = filePart.getMediaType() == null
                ? "text/csv"
                : filePart.getMediaType().toString();

        try (InputStream inputStream = filePart.getBody(InputStream.class, null)) {
            return new UploadedCsvFile(fileName, contentType, inputStream.readAllBytes());
        } catch (IOException e) {
            throw new BadRequestException("failed to read uploaded file");
        }
    }

    private String extractFileName(InputPart part) {
        if (part == null) {
            return "tickets-import.csv";
        }

        MultivaluedMap<String, String> headers = part.getHeaders();
        if (headers == null) {
            return "tickets-import.csv";
        }

        List<String> values = headers.get("Content-Disposition");
        if (values == null || values.isEmpty()) {
            return "tickets-import.csv";
        }

        Matcher matcher = FILENAME_PATTERN.matcher(values.get(0));
        if (!matcher.find()) {
            return "tickets-import.csv";
        }

        String candidate = matcher.group(1);
        if (candidate == null || candidate.isBlank()) {
            return "tickets-import.csv";
        }

        String normalized = candidate.replace("\\", "/");
        int idx = normalized.lastIndexOf('/');
        return idx >= 0 ? normalized.substring(idx + 1) : normalized;
    }

    private record UploadedCsvFile(String fileName, String contentType, byte[] body) {
    }
}


