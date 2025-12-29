package com.msgr.tickets.persistence;

import com.msgr.tickets.domain.entity.Event;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EventRepository {

    @PersistenceContext(unitName = "ticketsPU")
    private EntityManager em;

    public Optional<Event> findById(long id) {
        return Optional.ofNullable(em.find(Event.class, id));
    }

    public Event save(Event e) {
        if (e.getId() == null) {
            em.persist(e);
            return e;
        }
        return em.merge(e);
    }

    public void delete(Event e) {
        Event managed = em.contains(e) ? e : em.merge(e);
        em.remove(managed);
    }

    public long count() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Event> root = cq.from(Event.class);
        cq.select(cb.count(root));
        return em.createQuery(cq).getSingleResult();
    }

    public List<Event> findPage(int page, int size, String sort, String order) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Event> cq = cb.createQuery(Event.class);
        Root<Event> root = cq.from(Event.class);
        cq.select(root);

        Path<?> sortPath = switch (sort == null ? "id" : sort) {
            case "id" -> root.get("id");
            case "name" -> root.get("name");
            case "ticketsCount" -> root.get("ticketsCount");
            case "eventType" -> root.get("eventType");
            default -> root.get("id");
        };

        boolean desc = "desc".equalsIgnoreCase(order);
        cq.orderBy(desc ? cb.desc(sortPath) : cb.asc(sortPath));

        TypedQuery<Event> q = em.createQuery(cq);
        q.setFirstResult(page * size);
        q.setMaxResults(size);
        return q.getResultList();
    }
}
