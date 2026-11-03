package com.pos.common.security;

public interface TokenRevocations {

    boolean isRevoked(ParsedToken token);

    void revokeToken(ParsedToken token);

    void revokeUser(long userId);

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
