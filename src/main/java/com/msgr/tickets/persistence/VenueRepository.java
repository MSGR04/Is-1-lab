package com.msgr.tickets.persistence;

import com.msgr.tickets.domain.entity.Venue;
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
public class VenueRepository {

    @PersistenceContext(unitName = "ticketsPU")
    private EntityManager em;

    public Optional<Venue> findById(long id) {
        return Optional.ofNullable(em.find(Venue.class, id));
    }

    public Venue save(Venue v) {
        if (v.getId() == null) {
            em.persist(v);
            return v;
        }
        return em.merge(v);
    }

    public void delete(Venue v) {
        Venue managed = em.contains(v) ? v : em.merge(v);
        em.remove(managed);
    }

    public long count() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Venue> root = cq.from(Venue.class);
        cq.select(cb.count(root));
        return em.createQuery(cq).getSingleResult();
    }

    public List<Venue> findPage(int page, int size, String sort, String order) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Venue> cq = cb.createQuery(Venue.class);
        Root<Venue> root = cq.from(Venue.class);
        cq.select(root);

        Path<?> sortPath = switch (sort == null ? "id" : sort) {
            case "id" -> root.get("id");
            case "name" -> root.get("name");
            case "capacity" -> root.get("capacity");
            case "type" -> root.get("type");
            default -> root.get("id");
        };

        boolean desc = "desc".equalsIgnoreCase(order);
        cq.orderBy(desc ? cb.desc(sortPath) : cb.asc(sortPath));

        TypedQuery<Venue> q = em.createQuery(cq);
        q.setFirstResult(page * size);
        q.setMaxResults(size);
        return q.getResultList();
    }
}
