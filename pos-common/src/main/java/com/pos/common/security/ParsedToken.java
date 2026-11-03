package com.pos.common.security;

import java.time.Instant;

public record ParsedToken(AuthenticatedUser user, String jti, Instant issuedAt, Instant expiresAt) {
}
