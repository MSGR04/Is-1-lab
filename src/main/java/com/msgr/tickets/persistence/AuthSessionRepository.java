package com.msgr.tickets.persistence;

import com.msgr.tickets.domain.entity.AuthSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@ApplicationScoped
public class AuthSessionRepository {

    @PersistenceContext(unitName = "ticketsPU")
    private EntityManager em;

    public Optional<AuthSession> findValidByToken(String token) {
        try {
            AuthSession s = em.createQuery(
                    "select s from AuthSession s where s.token = :token and s.expiresAt > :now",
                    AuthSession.class
            )
                    .setParameter("token", token)
                    .setParameter("now", LocalDateTime.now())
                    .getSingleResult();
            return Optional.of(s);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public AuthSession save(AuthSession session) {
        if (em.find(AuthSession.class, session.getToken()) == null) {
            em.persist(session);
            return session;
        }
        return em.merge(session);
    }

    @Transactional
    public int deleteByToken(String token) {
        return em.createQuery("delete from AuthSession s where s.token = :token")
                .setParameter("token", token)
                .executeUpdate();
    }

    @Transactional
    public int deleteExpired() {
        return em.createQuery("delete from AuthSession s where s.expiresAt <= :now")
                .setParameter("now", LocalDateTime.now())
                .executeUpdate();
    }
}
