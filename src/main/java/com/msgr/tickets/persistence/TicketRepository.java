package com.msgr.tickets.persistence;
import jakarta.transaction.Transactional;
import com.msgr.tickets.domain.entity.Ticket;
import com.msgr.tickets.domain.enums.TicketType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TicketRepository {

    @PersistenceContext(unitName = "ticketsPU")
    private EntityManager em;

    public long countByEventId(long eventId) {
        return em.createQuery(
                "select count(t) from Ticket t where t.event.id = :eid",
                Long.class
        ).setParameter("eid", eventId).getSingleResult();
    }

    public int rebindEvent(long fromEventId, long toEventId) {
        return em.createQuery(
                        "update Ticket t set t.event.id = :to where t.event.id = :from"
                )
                .setParameter("to", toEventId)
                .setParameter("from", fromEventId)
                .executeUpdate();
    }

    @Transactional
    public int rebindPerson(long oldPersonId, long newPersonId) {
        return em.createQuery("""
            update Ticket t
            set t.person = (select p from Person p where p.id = :newId)
            where t.person.id = :oldId
        """)
                .setParameter("oldId", oldPersonId)
                .setParameter("newId", newPersonId)
                .executeUpdate();
    }

    @Transactional
    public int rebindVenue(long oldVenueId, long newVenueId) {
        return em.createQuery("""
            update Ticket t
            set t.venue = (select v from Venue v where v.id = :newId)
            where t.venue.id = :oldId
        """)
                .setParameter("oldId", oldVenueId)
                .setParameter("newId", newVenueId)
                .executeUpdate();
    }

    public Optional<Ticket> findById(long id) {
        return Optional.ofNullable(em.find(Ticket.class, id));
    }

    public Ticket save(Ticket t) {
        if (t.getId() == null) {
            em.persist(t);
            return t;
        }
        return em.merge(t);
    }

    public void delete(Ticket t) {
        Ticket managed = em.contains(t) ? t : em.merge(t);
        em.remove(managed);
    }

    public long count(
            Long id, String name, String comment, Integer coordinatesX, Long coordinatesY, String creationDate,
            Long personId, Long eventId, Long venueId,
            Double priceMin, Double priceMax, Double price, TicketType type, Long discount, Long number, Boolean refundable
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Ticket> root = cq.from(Ticket.class);

        cq.select(cb.count(root));

        List<Predicate> predicates = buildPredicates(
                cb, root,
                id, name, comment, coordinatesX, coordinatesY, creationDate,
                personId, eventId, venueId, priceMin, priceMax, price, type, discount, number, refundable
        );
        if (!predicates.isEmpty()) cq.where(predicates.toArray(new Predicate[0]));

        return em.createQuery(cq).getSingleResult();
    }

    public List<Ticket> findPage(
            int page, int size,
            String sort, String order,
            Long id, String name, String comment, Integer coordinatesX, Long coordinatesY, String creationDate,
            Long personId, Long eventId, Long venueId,
            Double priceMin, Double priceMax, Double price, TicketType type, Long discount, Long number, Boolean refundable
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Ticket> cq = cb.createQuery(Ticket.class);
        Root<Ticket> root = cq.from(Ticket.class);

        cq.select(root);

        List<Predicate> predicates = buildPredicates(
                cb, root,
                id, name, comment, coordinatesX, coordinatesY, creationDate,
                personId, eventId, venueId, priceMin, priceMax, price, type, discount, number, refundable
        );
        if (!predicates.isEmpty()) cq.where(predicates.toArray(new Predicate[0]));

        // сортировка (белый список полей)
        Path<?> sortPath = switch (sort == null ? "id" : sort) {
            case "id" -> root.get("id");
            case "name" -> root.get("name");
            case "coordinatesX" -> root.get("coordinates").get("x");
            case "coordinatesY" -> root.get("coordinates").get("y");
            case "price" -> root.get("price");
            case "creationDate" -> root.get("creationDate");
            case "discount" -> root.get("discount");
            case "number" -> root.get("number");
            case "comment" -> root.get("comment");
            case "type" -> root.get("type");
            case "personId" -> root.get("person").get("id");
            case "eventId" -> root.get("event").get("id");
            case "venueId" -> root.get("venue").get("id");
            case "refundable" -> root.get("refundable");
            default -> root.get("id");
        };

        boolean desc = "desc".equalsIgnoreCase(order);
        cq.orderBy(desc ? cb.desc(sortPath) : cb.asc(sortPath));

        TypedQuery<Ticket> q = em.createQuery(cq);
        q.setFirstResult(page * size);
        q.setMaxResults(size);
        return q.getResultList();
    }

    private List<Predicate> buildPredicates(
            CriteriaBuilder cb, Root<Ticket> root,
            Long id, String name, String comment, Integer coordinatesX, Long coordinatesY, String creationDate,
            Long personId, Long eventId, Long venueId,
            Double priceMin, Double priceMax, Double price, TicketType type, Long discount, Long number, Boolean refundable
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (id != null) {
            predicates.add(cb.equal(root.get("id"), id));
        }
        if (name != null && !name.isBlank()) {
            predicates.add(cb.equal(root.get("name"), name));
        }
        if (comment != null && !comment.isBlank()) {
            predicates.add(cb.equal(root.get("comment"), comment));
        }
        if (coordinatesX != null) {
            predicates.add(cb.equal(root.get("coordinates").get("x"), coordinatesX));
        }
        if (coordinatesY != null) {
            predicates.add(cb.equal(root.get("coordinates").get("y"), coordinatesY));
        }
        if (creationDate != null && !creationDate.isBlank()) {
            try {
                java.time.LocalDate d = java.time.LocalDate.parse(creationDate);
                predicates.add(cb.equal(root.get("creationDate"), d));
            } catch (java.time.format.DateTimeParseException ignored) {
            }
        }
        if (personId != null) {
            predicates.add(cb.equal(root.get("person").get("id"), personId));
        }
        if (eventId != null) {
            predicates.add(cb.equal(root.get("event").get("id"), eventId));
        }
        if (venueId != null) {
            predicates.add(cb.equal(root.get("venue").get("id"), venueId));
        }
        if (price != null) {
            predicates.add(cb.equal(root.get("price"), price));
        }
        if (discount != null) {
            predicates.add(cb.equal(root.get("discount"), discount));
        }
        if (number != null) {
            predicates.add(cb.equal(root.get("number"), number));
        }
        if (priceMin != null) predicates.add(cb.ge(root.get("price"), priceMin));
        if (priceMax != null) predicates.add(cb.le(root.get("price"), priceMax));
        if (type != null) predicates.add(cb.equal(root.get("type"), type));
        if (refundable != null) predicates.add(cb.equal(root.get("refundable"), refundable));

        return predicates;
    }
}
