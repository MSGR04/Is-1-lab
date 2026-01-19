package com.msgr.tickets.persistence;

import com.msgr.tickets.domain.entity.Venue;
import com.msgr.tickets.domain.enums.VenueType;
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

    public long count(
            Long id, String name, VenueType type, String zipCode, Integer capacity,
            Float townX, Float townY, Float townZ
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Venue> root = cq.from(Venue.class);
        cq.select(cb.count(root));
        java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
        if (id != null) predicates.add(cb.equal(root.get("id"), id));
        if (name != null && !name.isBlank()) predicates.add(cb.equal(root.get("name"), name));
        if (type != null) predicates.add(cb.equal(root.get("type"), type));
        if (zipCode != null && !zipCode.isBlank()) {
            predicates.add(cb.equal(root.get("address").get("zipCode"), zipCode));
        }
        if (capacity != null) predicates.add(cb.equal(root.get("capacity"), capacity));
        if (townX != null) predicates.add(cb.equal(root.get("address").get("town").get("x"), townX));
        if (townY != null) predicates.add(cb.equal(root.get("address").get("town").get("y"), townY));
        if (townZ != null) predicates.add(cb.equal(root.get("address").get("town").get("z"), townZ));
        if (!predicates.isEmpty()) cq.where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        return em.createQuery(cq).getSingleResult();
    }

    public List<Venue> findPage(
            int page, int size, String sort, String order,
            Long id, String name, VenueType type, String zipCode, Integer capacity,
            Float townX, Float townY, Float townZ
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Venue> cq = cb.createQuery(Venue.class);
        Root<Venue> root = cq.from(Venue.class);
        cq.select(root);
        java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
        if (id != null) predicates.add(cb.equal(root.get("id"), id));
        if (name != null && !name.isBlank()) predicates.add(cb.equal(root.get("name"), name));
        if (type != null) predicates.add(cb.equal(root.get("type"), type));
        if (zipCode != null && !zipCode.isBlank()) {
            predicates.add(cb.equal(root.get("address").get("zipCode"), zipCode));
        }
        if (capacity != null) predicates.add(cb.equal(root.get("capacity"), capacity));
        if (townX != null) predicates.add(cb.equal(root.get("address").get("town").get("x"), townX));
        if (townY != null) predicates.add(cb.equal(root.get("address").get("town").get("y"), townY));
        if (townZ != null) predicates.add(cb.equal(root.get("address").get("town").get("z"), townZ));
        if (!predicates.isEmpty()) cq.where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));

        Path<?> sortPath = switch (sort == null ? "id" : sort) {
            case "id" -> root.get("id");
            case "name" -> root.get("name");
            case "capacity" -> root.get("capacity");
            case "type" -> root.get("type");
            case "zipCode" -> root.get("address").get("zipCode");
            case "townX" -> root.get("address").get("town").get("x");
            case "townY" -> root.get("address").get("town").get("y");
            case "townZ" -> root.get("address").get("town").get("z");
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
