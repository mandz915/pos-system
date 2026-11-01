package com.pos.common.security;

import java.util.Set;

/**
 * The identity extracted from a validated JWT.
 *
 * @param storeId  the store the user is bound to, or {@code null} for head-office users who may access every store
 * @param tenantId the client company the user belongs to; {@code 0} for platform operators, who own no business data
 */
public record AuthenticatedUser(Long userId, String username, Set<String> roles, Long storeId, Long tenantId) {

    public boolean hasAnyRole(String... wanted) {
        for (String role : wanted) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /** Administrator of ONE client company (all of its stores). */
    public boolean isAdmin() {
        return roles.contains(Roles.ADMIN);
    }

    /** Operator of the whole platform: creates and suspends client companies. */
    public boolean isPlatformAdmin() {
        return roles.contains(Roles.PLATFORM_ADMIN);
    }
}
