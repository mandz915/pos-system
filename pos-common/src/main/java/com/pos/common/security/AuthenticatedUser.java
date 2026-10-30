package com.pos.common.security;

import java.util.Set;

/**
 * The identity extracted from a validated JWT.
 *
 * @param storeId the store the user is bound to, or {@code null} for head-office users who may access every store
 */
public record AuthenticatedUser(Long userId, String username, Set<String> roles, Long storeId) {

    public boolean hasAnyRole(String... wanted) {
        for (String role : wanted) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAdmin() {
        return roles.contains(Roles.ADMIN);
    }
}
