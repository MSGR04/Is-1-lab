package com.msgr.tickets.service;

import com.msgr.tickets.network.ws.TicketWsEndpoint;
import com.msgr.tickets.network.ws.TicketWsMessage;
import com.msgr.tickets.network.dto.PageDto;
import com.msgr.tickets.network.dto.TicketDto;
import com.msgr.tickets.network.dto.TicketImportHistoryDto;
import com.msgr.tickets.network.dto.TicketImportResultDto;
import com.msgr.tickets.network.dto.TicketUpsertDto;
import com.msgr.tickets.domain.embeddable.Address;
import com.msgr.tickets.domain.embeddable.Coordinates;
import com.msgr.tickets.domain.embeddable.Location;
import com.msgr.tickets.domain.entity.Event;
import com.msgr.tickets.domain.entity.Person;
import com.msgr.tickets.domain.entity.Ticket;
import com.msgr.tickets.domain.entity.Venue;
import com.msgr.tickets.domain.enums.Color;
import com.msgr.tickets.domain.enums.Country;
import com.msgr.tickets.domain.enums.EventType;
import com.msgr.tickets.domain.enums.TicketType;
import com.msgr.tickets.domain.enums.VenueType;
import com.msgr.tickets.network.mapper.CoordinatesMapper;
import com.msgr.tickets.persistence.TicketRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class TicketService {

    @Inject
    private TicketRepository repo;
    @Inject
    private CoordinatesMapper coordinatesMapper;

    @Inject private PersonService personService;
    @Inject private EventService eventService;
    @Inject private VenueService venueService;
    @Inject private Validator validator;
    @Inject private ImportOperationService importOperationService;

    @PersistenceContext(unitName = "ticketsPU")
    private EntityManager em;

    public TicketDto get(long id) {
        Ticket t = repo.findById(id).orElseThrow(() -> new NotFoundException("ticket not found: " + id));
        return toDto(t);
    }

    public PageDto<TicketDto> list(
            int page, int size,
            String sort, String order,
            Long id, String name, String comment, Integer coordinatesX, Long coordinatesY, String creationDate,
            Long personId, Long eventId, Long venueId,
            Double priceMin, Double priceMax, Double price, TicketType type, Long discount, Long number, Boolean refundable
    ) {
        long total = repo.count(
                id, name, comment, coordinatesX, coordinatesY, creationDate,
                personId, eventId, venueId, priceMin, priceMax, price, type, discount, number, refundable
        );
        List<TicketDto> items = repo.findPage(
                page, size, sort, order,
                id, name, comment, coordinatesX, coordinatesY, creationDate,
                personId, eventId, venueId, priceMin, priceMax, price, type, discount, number, refundable
        )
                .stream().map(this::toDto).toList();
        return new PageDto<>(items, total, page, size);
    }

    @Transactional
    public TicketDto create(TicketUpsertDto in) {
        Ticket t = new Ticket();
        applyUpsert(t, in);
        t.setCreationDate(LocalDate.now());
        validateUniqueTicketFields(t, null, null, null, null);
        repo.save(t);

        TicketWsEndpoint.broadcast(new TicketWsMessage("CREATED", t.getId()));
        return toDto(t);
    }

    @Transactional
    public TicketDto update(long id, TicketUpsertDto in) {
        Ticket t = repo.findById(id).orElseThrow(() -> new NotFoundException("ticket not found: " + id));
        applyUpsert(t, in);
        validateUniqueTicketFields(t, id, null, null, null);
        t = repo.save(t);

        TicketWsEndpoint.broadcast(new TicketWsMessage("UPDATED", t.getId()));
        return toDto(t);
    }

    @Transactional
    public void delete(long id) {
        Ticket t = repo.findById(id).orElseThrow(() -> new NotFoundException("ticket not found: " + id));
        repo.delete(t);

        TicketWsEndpoint.broadcast(new TicketWsMessage("DELETED", id));
    }

    @Transactional
    public TicketImportResultDto importCsv(long userId, String csvBody) {
        long operationId = importOperationService.createStarted(userId);

        try {
            CsvTable csvTable = parseCsv(csvBody);
            if (csvTable.rows().isEmpty()) {
                throw new BadRequestException("csv file has no data rows");
            }

            Set<String> importNames = new HashSet<>();
            Set<String> importCoordinates = new HashSet<>();
            int dataRowNumber = 2;
            for (Map<String, String> row : csvTable.rows()) {
                Ticket ticket = buildTicketFromCsv(row, dataRowNumber);
                validateUniqueTicketFields(ticket, null, dataRowNumber, importNames, importCoordinates);
                repo.save(ticket);
                dataRowNumber++;
            }

            em.flush();
            importOperationService.markSuccess(operationId, csvTable.rows().size());
            TicketWsEndpoint.broadcast(new TicketWsMessage("BULK_CREATED", null));
            return new TicketImportResultDto(csvTable.rows().size());
        } catch (RuntimeException e) {
            importOperationService.markFailed(operationId);
            throw e;
        }
    }

    public List<TicketImportHistoryDto> listImportHistory(long requesterUserId, String requesterRole) {
        return importOperationService.listHistory(requesterUserId, requesterRole);
    }

    private void applyUpsert(Ticket t, TicketUpsertDto in) {
        t.setName(in.getName());
        t.setCoordinates(coordinatesMapper.toEmbeddable(in.getCoordinates()));

        Long personId = in.getPersonId();
        if (personId != null && personId <= 0) {
            personId = null;
        }

        Person person = null;
        if (personId != null) {
            person = em.find(Person.class, personId);
            if (person == null) throw new NotFoundException("person not found: " + personId);
        }

        Event event = em.find(Event.class, in.getEventId());
        if (event == null) throw new NotFoundException("event not found: " + in.getEventId());

        Venue venue = null;
        if (in.getVenueId() != null) {
            venue = em.find(Venue.class, in.getVenueId());
            if (venue == null) throw new NotFoundException("venue not found: " + in.getVenueId());
        }

        t.setPerson(person);
        t.setEvent(event);
        t.setVenue(venue);

        t.setPrice(in.getPrice());
        t.setType(in.getType());
        t.setDiscount(in.getDiscount());
        t.setNumber(in.getNumber());
        t.setComment(in.getComment());
        t.setRefundable(in.isRefundable());
    }

    private Ticket buildTicketFromCsv(Map<String, String> row, int rowNumber) {
        Event event = buildEventFromCsv(row, rowNumber);
        validateEntity(event, rowNumber, "event");
        em.persist(event);

        Person person = buildPersonFromCsv(row, rowNumber);
        if (person != null) {
            validateEntity(person, rowNumber, "person");
            em.persist(person);
        }

        Venue venue = buildVenueFromCsv(row, rowNumber);
        if (venue != null) {
            validateEntity(venue, rowNumber, "venue");
            em.persist(venue);
        }

        Ticket ticket = new Ticket();
        ticket.setName(getRequiredString(row, "name", rowNumber));
        ticket.setCoordinates(new Coordinates(
                getRequiredInt(row, "coordinates.x", rowNumber),
                getRequiredLong(row, "coordinates.y", rowNumber)
        ));
        ticket.setCreationDate(LocalDate.now());
        ticket.setPerson(person);
        ticket.setEvent(event);
        ticket.setVenue(venue);
        ticket.setPrice(getRequiredDouble(row, "price", rowNumber));
        ticket.setType(getRequiredEnum(row, "type", TicketType.class, rowNumber));
        ticket.setDiscount(getRequiredLong(row, "discount", rowNumber));
        ticket.setNumber(getRequiredLong(row, "number", rowNumber));
        ticket.setComment(getRequiredString(row, "comment", rowNumber));
        ticket.setRefundable(getRequiredBoolean(row, "refundable", rowNumber));

        validateEntity(ticket, rowNumber, "ticket");
        return ticket;
    }

    private Event buildEventFromCsv(Map<String, String> row, int rowNumber) {
        Event event = new Event();
        event.setName(getRequiredString(row, "event.name", rowNumber));
        event.setTicketsCount(getOptionalInt(row, "event.ticketsCount", rowNumber));
        event.setEventType(getRequiredEnum(row, "event.eventType", EventType.class, rowNumber));
        return event;
    }

    private Person buildPersonFromCsv(Map<String, String> row, int rowNumber) {
        if (!hasAnyValueWithPrefix(row, "person.")) {
            return null;
        }

        Person person = new Person();
        person.setEyeColor(getOptionalEnum(row, "person.eyeColor", Color.class, rowNumber));
        person.setHairColor(getOptionalEnum(row, "person.hairColor", Color.class, rowNumber));
        person.setLocation(new Location(
                getRequiredFloat(row, "person.location.x", rowNumber),
                getRequiredFloat(row, "person.location.y", rowNumber),
                getRequiredFloat(row, "person.location.z", rowNumber)
        ));
        person.setPassportID(getRequiredString(row, "person.passportID", rowNumber));
        person.setNationality(getRequiredEnum(row, "person.nationality", Country.class, rowNumber));
        return person;
    }

    private Venue buildVenueFromCsv(Map<String, String> row, int rowNumber) {
        if (!hasAnyValueWithPrefix(row, "venue.")) {
            return null;
        }

        Venue venue = new Venue();
        venue.setName(getRequiredString(row, "venue.name", rowNumber));
        venue.setCapacity(getRequiredInt(row, "venue.capacity", rowNumber));
        venue.setType(getRequiredEnum(row, "venue.type", VenueType.class, rowNumber));

        Address address = new Address();
        address.setZipCode(getOptionalString(row, "venue.address.zipCode"));

        boolean hasTown = hasAnyValue(row,
                "venue.address.town.x",
                "venue.address.town.y",
                "venue.address.town.z"
        );
        if (hasTown) {
            address.setTown(new Location(
                    getRequiredFloat(row, "venue.address.town.x", rowNumber),
                    getRequiredFloat(row, "venue.address.town.y", rowNumber),
                    getRequiredFloat(row, "venue.address.town.z", rowNumber)
            ));
        }

        venue.setAddress(address);
        return venue;
    }

    private CsvTable parseCsv(String csvBody) {
        if (csvBody == null || csvBody.isBlank()) {
            throw new BadRequestException("csv body is empty");
        }

        String source = csvBody;
        if (!source.isEmpty() && source.charAt(0) == '\uFEFF') {
            source = source.substring(1);
        }

        char delimiter = detectDelimiter(source);
        List<List<String>> rows = parseCsvRows(source, delimiter);

        List<List<String>> nonEmptyRows = new ArrayList<>();
        for (List<String> row : rows) {
            boolean hasData = row.stream().anyMatch(cell -> cell != null && !cell.trim().isEmpty());
            if (hasData) {
                nonEmptyRows.add(row);
            }
        }

        if (nonEmptyRows.isEmpty()) {
            throw new BadRequestException("csv file has no rows");
        }

        List<String> headers = nonEmptyRows.get(0);
        List<String> normalizedHeaders = new ArrayList<>(headers.size());
        Set<String> uniqueHeaders = new HashSet<>();

        for (String header : headers) {
            String normalizedHeader = normalizeHeader(header);
            if (normalizedHeader.isBlank()) {
                throw new BadRequestException("csv header contains empty column name");
            }
            if (!uniqueHeaders.add(normalizedHeader)) {
                throw new BadRequestException("csv header contains duplicate column: " + normalizedHeader);
            }
            normalizedHeaders.add(normalizedHeader);
        }

        List<String> requiredHeaders = List.of(
                "name",
                "coordinates.x",
                "coordinates.y",
                "price",
                "type",
                "discount",
                "number",
                "comment",
                "refundable",
                "event.name",
                "event.eventtype"
        );

        List<String> missingHeaders = requiredHeaders.stream()
                .filter(required -> !uniqueHeaders.contains(required))
                .toList();
        if (!missingHeaders.isEmpty()) {
            throw new BadRequestException("csv header is missing required columns: " + String.join(", ", missingHeaders));
        }

        List<Map<String, String>> dataRows = new ArrayList<>();
        for (int i = 1; i < nonEmptyRows.size(); i++) {
            List<String> values = nonEmptyRows.get(i);
            Map<String, String> row = new HashMap<>();
            for (int col = 0; col < normalizedHeaders.size(); col++) {
                String header = normalizedHeaders.get(col);
                String value = col < values.size() ? values.get(col) : "";
                row.put(header, value == null ? "" : value.trim());
            }
            dataRows.add(row);
        }

        return new CsvTable(dataRows);
    }

    private char detectDelimiter(String source) {
        int commas = 0;
        int semicolons = 0;
        boolean inQuotes = false;

        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < source.length() && source.charAt(i + 1) == '"') {
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (!inQuotes && (c == '\n' || c == '\r')) {
                break;
            }
            if (!inQuotes && c == ',') commas++;
            if (!inQuotes && c == ';') semicolons++;
        }

        return semicolons > commas ? ';' : ',';
    }

    private List<List<String>> parseCsvRows(String source, char delimiter) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < source.length() && source.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (!inQuotes && c == delimiter) {
                row.add(cell.toString());
                cell.setLength(0);
                continue;
            }

            if (!inQuotes && (c == '\n' || c == '\r')) {
                if (c == '\r' && i + 1 < source.length() && source.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(cell.toString());
                cell.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
                continue;
            }

            cell.append(c);
        }

        if (inQuotes) {
            throw new BadRequestException("csv parse error: unclosed quoted value");
        }

        row.add(cell.toString());
        rows.add(row);
        return rows;
    }

    private String getRequiredString(Map<String, String> row, String key, int rowNumber) {
        String value = getOptionalString(row, key);
        if (value == null || value.isBlank()) {
            throw badRowField(rowNumber, key, "is required");
        }
        return value;
    }

    private String getOptionalString(Map<String, String> row, String key) {
        String value = row.get(normalizeHeader(key));
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int getRequiredInt(Map<String, String> row, String key, int rowNumber) {
        String value = getRequiredString(row, key, rowNumber);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw badRowField(rowNumber, key, "must be an integer");
        }
    }

    private Integer getOptionalInt(Map<String, String> row, String key, int rowNumber) {
        String value = getOptionalString(row, key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw badRowField(rowNumber, key, "must be an integer");
        }
    }

    private long getRequiredLong(Map<String, String> row, String key, int rowNumber) {
        String value = getRequiredString(row, key, rowNumber);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw badRowField(rowNumber, key, "must be an integer");
        }
    }

    private double getRequiredDouble(Map<String, String> row, String key, int rowNumber) {
        String value = getRequiredString(row, key, rowNumber);
        try {
            if (value.contains(",") && !value.contains(".")) {
                value = value.replace(',', '.');
            }
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw badRowField(rowNumber, key, "must be a number");
        }
    }

    private float getRequiredFloat(Map<String, String> row, String key, int rowNumber) {
        String value = getRequiredString(row, key, rowNumber);
        try {
            if (value.contains(",") && !value.contains(".")) {
                value = value.replace(',', '.');
            }
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw badRowField(rowNumber, key, "must be a number");
        }
    }

    private boolean getRequiredBoolean(Map<String, String> row, String key, int rowNumber) {
        String value = getRequiredString(row, key, rowNumber).toLowerCase(Locale.ROOT);
        return switch (value) {
            case "true", "1", "yes" -> true;
            case "false", "0", "no" -> false;
            default -> throw badRowField(rowNumber, key, "must be true/false");
        };
    }

    private <E extends Enum<E>> E getRequiredEnum(Map<String, String> row, String key, Class<E> enumClass, int rowNumber) {
        String value = getRequiredString(row, key, rowNumber);
        return parseEnumValue(value, key, enumClass, rowNumber);
    }

    private <E extends Enum<E>> E getOptionalEnum(Map<String, String> row, String key, Class<E> enumClass, int rowNumber) {
        String value = getOptionalString(row, key);
        if (value == null) {
            return null;
        }
        return parseEnumValue(value, key, enumClass, rowNumber);
    }

    private <E extends Enum<E>> E parseEnumValue(String value, String key, Class<E> enumClass, int rowNumber) {
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        try {
            return Enum.valueOf(enumClass, normalized);
        } catch (IllegalArgumentException e) {
            throw badRowField(
                    rowNumber,
                    key,
                    "must be one of: " + Arrays.toString(enumClass.getEnumConstants())
            );
        }
    }

    private boolean hasAnyValueWithPrefix(Map<String, String> row, String prefix) {
        String normalizedPrefix = normalizeHeader(prefix);
        return row.entrySet().stream()
                .anyMatch(e -> e.getKey().startsWith(normalizedPrefix) && e.getValue() != null && !e.getValue().isBlank());
    }

    private boolean hasAnyValue(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = getOptionalString(row, key);
            if (value != null) {
                return true;
            }
        }
        return false;
    }

    private String normalizeHeader(String header) {
        return header == null ? "" : header.trim().toLowerCase(Locale.ROOT);
    }

    private BadRequestException badRowField(int rowNumber, String field, String reason) {
        return new BadRequestException("row " + rowNumber + ", field '" + field + "' " + reason);
    }

    private void validateUniqueTicketFields(
            Ticket ticket,
            Long excludeId,
            Integer rowNumber,
            Set<String> importNames,
            Set<String> importCoordinates
    ) {
        String name = ticket.getName();
        if (name != null && !name.isBlank()) {
            if (importNames != null && !importNames.add(name)) {
                throw uniquenessViolation("name", rowNumber);
            }
            if (repo.existsByName(name, excludeId)) {
                throw uniquenessViolation("name", rowNumber);
            }
        }

        Coordinates coordinates = ticket.getCoordinates();
        if (coordinates != null) {
            String coordinatesKey = coordinatesKey(coordinates);
            if (importCoordinates != null && !importCoordinates.add(coordinatesKey)) {
                throw uniquenessViolation("coordinates", rowNumber);
            }
            if (repo.existsByCoordinates(coordinates.getX(), coordinates.getY(), excludeId)) {
                throw uniquenessViolation("coordinates", rowNumber);
            }
        }
    }

    private String coordinatesKey(Coordinates coordinates) {
        return coordinates.getX() + ":" + coordinates.getY();
    }

    private BadRequestException uniquenessViolation(String field, Integer rowNumber) {
        if (rowNumber != null) {
            return badRowField(rowNumber, field, "must be unique");
        }
        return new BadRequestException("field '" + field + "' must be unique");
    }

    private <T> void validateEntity(T entity, int rowNumber, String objectName) {
        Set<ConstraintViolation<T>> violations = validator.validate(entity);
        if (violations.isEmpty()) {
            return;
        }

        ConstraintViolation<T> firstViolation = violations.iterator().next();
        String path = firstViolation.getPropertyPath() == null ? "" : firstViolation.getPropertyPath().toString();
        String pathPart = path.isBlank() ? objectName : objectName + "." + path;

        throw new BadRequestException("row " + rowNumber + ", field '" + pathPart + "' " + firstViolation.getMessage());
    }

    private record CsvTable(List<Map<String, String>> rows) {}

    private TicketDto toDto(Ticket t) {
        TicketDto dto = new TicketDto();
        dto.setId(t.getId());
        dto.setName(t.getName());
        dto.setCoordinates(coordinatesMapper.toDto(t.getCoordinates()));
        dto.setCreationDate(t.getCreationDate());

        dto.setPersonId(t.getPerson() == null ? null : t.getPerson().getId());
        dto.setEventId(t.getEvent().getId());
        dto.setVenueId(t.getVenue() == null ? null : t.getVenue().getId());

        dto.setPerson(t.getPerson() == null ? null : personService.get(t.getPerson().getId()));
        dto.setEvent(eventService.get(t.getEvent().getId()));
        dto.setVenue(t.getVenue() == null ? null : venueService.get(t.getVenue().getId()));


        dto.setPrice(t.getPrice());
        dto.setType(t.getType());
        dto.setDiscount(t.getDiscount());
        dto.setNumber(t.getNumber());
        dto.setComment(t.getComment());
        dto.setRefundable(t.isRefundable());
        return dto;
    }
}
