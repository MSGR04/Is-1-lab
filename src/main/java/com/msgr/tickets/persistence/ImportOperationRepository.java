package com.msgr.tickets.persistence;

import com.msgr.tickets.domain.entity.ImportOperation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ImportOperationRepository {

    @PersistenceContext(unitName = "ticketsPU")
    private EntityManager em;

    public ImportOperation save(ImportOperation operation) {
        if (operation.getId() == null) {
            em.persist(operation);
            return operation;
        }
        return em.merge(operation);
    }

    public Optional<ImportOperation> findById(long id) {
        return Optional.ofNullable(em.find(ImportOperation.class, id));
    }

    public List<ImportOperation> findRecentAll(int limit) {
        return em.createQuery(
                        "select o from ImportOperation o join fetch o.user order by o.id desc",
                        ImportOperation.class
                )
                .setMaxResults(limit)
                .getResultList();
    }

    public List<ImportOperation> findRecentByUser(long userId, int limit) {
        return em.createQuery(
                        "select o from ImportOperation o join fetch o.user where o.user.id = :uid order by o.id desc",
                        ImportOperation.class
                )
                .setParameter("uid", userId)
                .setMaxResults(limit)
                .getResultList();
    }
}
