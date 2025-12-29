package com.msgr.tickets.persistence;

import com.msgr.tickets.domain.entity.Person;
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

    public long count() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Person> root = cq.from(Person.class);
        cq.select(cb.count(root));
        return em.createQuery(cq).getSingleResult();
    }

    public List<Person> findPage(int page, int size, String sort, String order) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Person> cq = cb.createQuery(Person.class);
        Root<Person> root = cq.from(Person.class);
        cq.select(root);

        Path<?> sortPath = switch (sort == null ? "id" : sort) {
            case "id" -> root.get("id");
            case "passportID" -> root.get("passportID");
            case "nationality" -> root.get("nationality");
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
