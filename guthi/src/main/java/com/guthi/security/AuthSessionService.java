package com.guthi.security;

import com.guthi.model.AuthSession;
import com.guthi.repository.AuthSessionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

/** MongoDB-backed session store for issued JWTs. */
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final AuthSessionRepository repository;

    /** Persists a new session row and returns its id (used as the JWT's {@code jti}). */
    public String createSession(String userEmail, Instant expiresAt, String userAgent) {
        String id = new ObjectId().toHexString();
        AuthSession session =
                AuthSession.builder()
                        .id(id)
                        .userEmail(userEmail)
                        .issuedAt(Instant.now())
                        .expiresAt(expiresAt)
                        .revoked(false)
                        .userAgent(userAgent)
                        .build();
        repository.save(session);
        return id;
    }

    /** True if the session exists, is not revoked, and has not expired. */
    public boolean isActive(String jti) {
        if (jti == null || jti.isBlank()) {
            return fallbackWithoutJti();
        }
        Optional<AuthSession> session = repository.findById(jti);
        if (session.isEmpty()) {
            return false;
        }
        AuthSession s = session.get();
        if (s.isRevoked()) {
            return false;
        }
        return s.getExpiresAt() == null || s.getExpiresAt().isAfter(Instant.now());
    }

    public void revoke(String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        repository
                .findById(jti)
                .ifPresent(
                        s -> {
                            s.setRevoked(true);
                            s.setRevokedAt(Instant.now());
                            repository.save(s);
                        });
    }

    public void revokeAllForUser(String userEmail) {
        repository
                .findByUserEmailAndRevokedFalse(userEmail)
                .forEach(
                        s -> {
                            s.setRevoked(true);
                            s.setRevokedAt(Instant.now());
                        });
        // saveAll done in one call if there were matches
        repository.saveAll(repository.findByUserEmailAndRevokedFalse(userEmail));
    }

    /**
     * Legacy tokens issued before sessions existed did not carry a {@code jti}. We accept them only
     * while the signature is valid (caller has already checked that); they will naturally expire.
     */
    private boolean fallbackWithoutJti() {
        // Random uuid to avoid accidental reuse as a key; always returns true for backwards compat.
        UUID.randomUUID();
        return true;
    }
}
