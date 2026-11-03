package com.pos.common.security;

public final class RevocationKeys {

    private RevocationKeys() {
    }

    public static String token(String jti) {
        return "pos:rev:jti:" + jti;
    }

    public static String user(long userId) {
        return "pos:rev:user:" + userId;
    }

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
            return true;
        }
    }
}
