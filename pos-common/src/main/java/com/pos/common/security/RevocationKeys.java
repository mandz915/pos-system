package com.pos.common.security;

/** Redis key names of the token blocklist, shared by the servlet services and the reactive gateway. */
public final class RevocationKeys {

    private RevocationKeys() {
    }

    /** Holds "1" until the token expires. Present means: this single token is revoked (logout). */
    public static String token(String jti) {
        return "pos:rev:jti:" + jti;
    }

    /** Holds the epoch millis of the revocation. Every token issued at or before that moment is revoked. */
    public static String user(long userId) {
        return "pos:rev:user:" + userId;
    }

    /** Decides from the two values read from Redis (null = absent) whether the token is revoked. */
    public static boolean isRevoked(ParsedToken token, String tokenEntry, String userEntry) {
        if (tokenEntry != null) {
            return true;
        }
        if (userEntry == null) {
            return false;
        }
        try {
            return token.issuedAt().toEpochMilli() <= Long.parseLong(userEntry);
        } catch (NumberFormatException e) {
            return true; // unreadable marker: fail safe for this user
        }
    }
}
