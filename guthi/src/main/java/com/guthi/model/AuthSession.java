package com.guthi.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Persisted JWT session. The token's {@code jti} claim is the document id; the signed JWT itself is
 * never stored (only its metadata). Expired sessions are auto-removed via the TTL index.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "auth_sessions")
public class AuthSession {

    @Id
    private String id;

    @Indexed
    private String userEmail;

    private Instant issuedAt;

    @Indexed(expireAfterSeconds = 0)
    private Instant expiresAt;

    private boolean revoked;

    private Instant revokedAt;

    private String userAgent;
}
