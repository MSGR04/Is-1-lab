package com.msgr.tickets.persistence;

import com.msgr.tickets.domain.entity.Person;
import com.msgr.tickets.domain.enums.Color;
import com.msgr.tickets.domain.enums.Country;
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
public class PersonRepository {

    @PersistenceContext(unitName = "ticketsPU")
    private EntityManager em;

    public Optional<Person> findById(long id) {
        return Optional.ofNullable(em.find(Person.class, id));
    }

    public Person save(Person p) {
        if (p.getId() == null) {
            em.persist(p);
            return p;
        }
        return em.merge(p);
        
    }

    public void delete(Person p) {
        Person managed = em.contains(p) ? p : em.merge(p);
        em.remove(managed);
    }

    public long count(
            Long id, Color eyeColor, Color hairColor, String passportID, Country nationality,
            Float locationX, Float locationY, Float locationZ
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Person> root = cq.from(Person.class);
        cq.select(cb.count(root));
        java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
        if (id != null) predicates.add(cb.equal(root.get("id"), id));
        if (eyeColor != null) predicates.add(cb.equal(root.get("eyeColor"), eyeColor));
        if (hairColor != null) predicates.add(cb.equal(root.get("hairColor"), hairColor));
        if (passportID != null && !passportID.isBlank()) predicates.add(cb.equal(root.get("passportID"), passportID));
        if (nationality != null) predicates.add(cb.equal(root.get("nationality"), nationality));
        if (locationX != null) predicates.add(cb.equal(root.get("location").get("x"), locationX));
        if (locationY != null) predicates.add(cb.equal(root.get("location").get("y"), locationY));
        if (locationZ != null) predicates.add(cb.equal(root.get("location").get("z"), locationZ));
        if (!predicates.isEmpty()) cq.where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        return em.createQuery(cq).getSingleResult();
    }

    public List<Person> findPage(
            int page, int size, String sort, String order,
            Long id, Color eyeColor, Color hairColor, String passportID, Country nationality,
            Float locationX, Float locationY, Float locationZ
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Person> cq = cb.createQuery(Person.class);
        Root<Person> root = cq.from(Person.class);
        cq.select(root);
        java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
        if (id != null) predicates.add(cb.equal(root.get("id"), id));
        if (eyeColor != null) predicates.add(cb.equal(root.get("eyeColor"), eyeColor));
        if (hairColor != null) predicates.add(cb.equal(root.get("hairColor"), hairColor));
        if (passportID != null && !passportID.isBlank()) predicates.add(cb.equal(root.get("passportID"), passportID));
        if (nationality != null) predicates.add(cb.equal(root.get("nationality"), nationality));
        if (locationX != null) predicates.add(cb.equal(root.get("location").get("x"), locationX));
        if (locationY != null) predicates.add(cb.equal(root.get("location").get("y"), locationY));
        if (locationZ != null) predicates.add(cb.equal(root.get("location").get("z"), locationZ));
        if (!predicates.isEmpty()) cq.where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));

        Path<?> sortPath = switch (sort == null ? "id" : sort) {
            case "id" -> root.get("id");
            case "passportID" -> root.get("passportID");
            case "nationality" -> root.get("nationality");
            case "eyeColor" -> root.get("eyeColor");
            case "hairColor" -> root.get("hairColor");
            case "locationX" -> root.get("location").get("x");
            case "locationY" -> root.get("location").get("y");
            case "locationZ" -> root.get("location").get("z");
            default -> root.get("id");
        };

        boolean desc = "desc".equalsIgnoreCase(order);
        cq.orderBy(desc ? cb.desc(sortPath) : cb.asc(sortPath));

        TypedQuery<Person> q = em.createQuery(cq);
        q.setFirstResult(page * size);
        q.setMaxResults(size);
        return q.getResultList();
    }
}
