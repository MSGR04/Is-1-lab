package com.msgr.tickets.persistence;

import com.msgr.tickets.domain.entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import java.util.Optional;

@ApplicationScoped
public class AppUserRepository {

    @PersistenceContext(unitName = "ticketsPU")
    private EntityManager em;

    public Optional<AppUser> findById(long id) {
        return Optional.ofNullable(em.find(AppUser.class, id));
    }

    public Optional<AppUser> findByUsername(String username) {
        try {
            AppUser u = em.createQuery(
                    "select u from AppUser u where u.username = :username",
                    AppUser.class
            ).setParameter("username", username).getSingleResult();
            return Optional.of(u);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public AppUser save(AppUser user) {
        if (user.getId() == null) {
            em.persist(user);
            return user;
        }
        return em.merge(user);
    }
}
