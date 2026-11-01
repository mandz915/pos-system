package com.pos.common.security;

/**
 * Blocklist for tokens that are still cryptographically valid but must stop working (logout, disabled account,
 * password reset, role change). Backed by Redis; a no-op when Redis is not configured.
 */
public interface TokenRevocations {

    boolean isRevoked(ParsedToken token);

    /** Revokes one token (logout). The entry lives until the token would have expired anyway. */
    void revokeToken(ParsedToken token);

    /** Revokes every token the user holds right now; tokens issued afterwards (a new login) are unaffected. */
    void revokeUser(long userId);

    /** Used when Redis is absent: nothing is ever revoked, tokens simply expire. */
    TokenRevocations NONE = new TokenRevocations() {
        @Override
        public boolean isRevoked(ParsedToken token) {
            return false;
        }

        @Override
        public void revokeToken(ParsedToken token) {
        }

        @Override
        public void revokeUser(long userId) {
        }
    };
}
