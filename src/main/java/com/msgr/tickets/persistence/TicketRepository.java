package com.msgr.tickets.persistence;
import jakarta.transaction.Transactional;
import com.msgr.tickets.domain.entity.Ticket;
import com.msgr.tickets.domain.enums.TicketType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import com.msgr.tickets.domain.entity.Person;
import com.msgr.tickets.domain.entity.Venue;


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
        // JPQL bulk update
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

    public long count(String name, Double priceMin, Double priceMax, TicketType type, Boolean refundable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Ticket> root = cq.from(Ticket.class);

        cq.select(cb.count(root));

        List<Predicate> predicates = buildPredicates(cb, root, name, priceMin, priceMax, type, refundable);
        if (!predicates.isEmpty()) cq.where(predicates.toArray(new Predicate[0]));

        return em.createQuery(cq).getSingleResult();
    }

    public List<Ticket> findPage(
            int page, int size,
            String sort, String order,
            String name, Double priceMin, Double priceMax, TicketType type, Boolean refundable
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Ticket> cq = cb.createQuery(Ticket.class);
        Root<Ticket> root = cq.from(Ticket.class);

        cq.select(root);

        List<Predicate> predicates = buildPredicates(cb, root, name, priceMin, priceMax, type, refundable);
        if (!predicates.isEmpty()) cq.where(predicates.toArray(new Predicate[0]));

        // сортировка (белый список полей)
        Path<?> sortPath = switch (sort == null ? "id" : sort) {
            case "id" -> root.get("id");
            case "name" -> root.get("name");
            case "price" -> root.get("price");
            case "creationDate" -> root.get("creationDate");
            case "discount" -> root.get("discount");
            case "number" -> root.get("number");
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
            String name, Double priceMin, Double priceMax, TicketType type, Boolean refundable
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (name != null && !name.isBlank()) {
            predicates.add(cb.equal(root.get("name"), name));
        }
        if (priceMin != null) predicates.add(cb.ge(root.get("price"), priceMin));
        if (priceMax != null) predicates.add(cb.le(root.get("price"), priceMax));
        if (type != null) predicates.add(cb.equal(root.get("type"), type));
        if (refundable != null) predicates.add(cb.equal(root.get("refundable"), refundable));

        return predicates;
    }
}
