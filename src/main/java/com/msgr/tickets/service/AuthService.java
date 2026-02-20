package com.msgr.tickets.service;

import com.msgr.tickets.domain.entity.AppUser;
import com.msgr.tickets.domain.entity.AuthSession;
import com.msgr.tickets.network.dto.AuthBootstrapAdminResultDto;
import com.msgr.tickets.network.dto.AuthUserDto;
import com.msgr.tickets.persistence.AppUserRepository;
import com.msgr.tickets.persistence.AuthSessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    private static final int ITERATIONS = 65_536;
    private static final int KEY_LEN_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final int SESSION_HOURS = 24;

    @Inject
    private AppUserRepository userRepo;

    @Inject
    private AuthSessionRepository sessionRepo;

    @Transactional
    public AuthUserDto register(String usernameRaw, String passwordRaw) {
        String username = normalize(usernameRaw);
        String password = passwordRaw == null ? "" : passwordRaw.trim();

        if (username.length() < 3) throw new BadRequestException("username must be at least 3 chars");
        if (password.length() < 6) throw new BadRequestException("password must be at least 6 chars");
        if (userRepo.findByUsername(username).isPresent()) {
            throw new BadRequestException("username already exists");
        }

        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPasswordHash(hashPassword(password));
        u.setRole("USER");
        userRepo.save(u);

        return toDto(u);
    }

    @Transactional
    public String login(String usernameRaw, String passwordRaw) {
        String username = normalize(usernameRaw);
        String password = passwordRaw == null ? "" : passwordRaw.trim();

        AppUser u = userRepo.findByUsername(username)
                .orElseThrow(() -> new NotAuthorizedException("invalid credentials"));

        if (!verifyPassword(password, u.getPasswordHash())) {
            throw new NotAuthorizedException("invalid credentials");
        }

        sessionRepo.deleteExpired();

        String token = UUID.randomUUID().toString().replace("-", "");
        AuthSession s = new AuthSession();
        s.setToken(token);
        s.setUser(u);
        s.setExpiresAt(LocalDateTime.now().plusHours(SESSION_HOURS));
        sessionRepo.save(s);

        return token;
    }

    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank()) return;
        sessionRepo.deleteByToken(token);
    }

    public Optional<AuthUserDto> resolveUser(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return sessionRepo.findValidByToken(token).map(s -> toDto(s.getUser()));
    }

    @Transactional
    public AuthUserDto grantAdmin(String requesterToken, String targetUsernameRaw) {
        AppUser requester = sessionRepo.findValidByToken(requesterToken)
                .map(AuthSession::getUser)
                .orElseThrow(() -> new NotAuthorizedException("unauthorized"));

        String targetUsername = normalize(targetUsernameRaw);
        if (targetUsername.length() < 3) {
            throw new BadRequestException("username must be at least 3 chars");
        }

        AppUser targetUser = userRepo.findByUsername(targetUsername)
                .orElseThrow(() -> new BadRequestException("user not found"));

        boolean requesterIsAdmin = "ADMIN".equalsIgnoreCase(requester.getRole());
        boolean hasAnyAdmin = userRepo.countByRole("ADMIN") > 0;
        boolean selfBootstrap = !hasAnyAdmin && requester.getId().equals(targetUser.getId());

        if (!requesterIsAdmin && !selfBootstrap) {
            throw new ForbiddenException("admin role required");
        }

        targetUser.setRole("ADMIN");
        userRepo.save(targetUser);
        return toDto(targetUser);
    }

    @Transactional
    public AuthBootstrapAdminResultDto bootstrapAdmin(String requesterToken) {
        AppUser requester = sessionRepo.findValidByToken(requesterToken)
                .map(AuthSession::getUser)
                .orElseThrow(() -> new NotAuthorizedException("unauthorized"));

        if ("ADMIN".equalsIgnoreCase(requester.getRole())) {
            return new AuthBootstrapAdminResultDto(true, "already admin", toDto(requester));
        }

        boolean hasAnyAdmin = userRepo.countByRole("ADMIN") > 0;
        if (hasAnyAdmin) {
            return new AuthBootstrapAdminResultDto(false, "\u0430\u0434\u043c\u0438\u043d \u0443\u0436\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442", toDto(requester));
        }

        requester.setRole("ADMIN");
        userRepo.save(requester);
        return new AuthBootstrapAdminResultDto(true, "role granted", toDto(requester));
    }

    private AuthUserDto toDto(AppUser u) {
        return new AuthUserDto(u.getId(), u.getUsername(), u.getRole());
    }

    private static String normalize(String usernameRaw) {
        return usernameRaw == null ? "" : usernameRaw.trim();
    }

    private static String hashPassword(String password) {
        try {
            byte[] salt = new byte[SALT_BYTES];
            new SecureRandom().nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LEN_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            return ITERATIONS + ":" + HexFormat.of().formatHex(salt) + ":" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("password hashing failed", e);
        }
    }

    private static boolean verifyPassword(String password, String stored) {
        try {
            String[] parts = stored.split(":");
            if (parts.length != 3) return false;

            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = HexFormat.of().parseHex(parts[1]);
            byte[] expected = HexFormat.of().parseHex(parts[2]);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, expected.length * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] actual = skf.generateSecret(spec).getEncoded();

            if (actual.length != expected.length) return false;
            int diff = 0;
            for (int i = 0; i < expected.length; i++) {
                diff |= (actual[i] ^ expected[i]);
            }
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
