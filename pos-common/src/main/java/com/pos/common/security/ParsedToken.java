package com.pos.common.security;

import java.time.Instant;

/**
 * A validated token: the identity plus the data needed to revoke it.
 *
 * @param jti       unique token id (used to revoke a single token on logout)
 * @param issuedAt  millisecond precision issue time (used to revoke every token a user held before a point in time)
 * @param expiresAt when the token stops being valid anyway, so a revocation entry never needs to outlive it
 */
public record ParsedToken(AuthenticatedUser user, String jti, Instant issuedAt, Instant expiresAt) {
}
