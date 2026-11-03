package com.pos.common.security;

import java.util.Set;

public record AuthenticatedUser(Long userId, String username, Set<String> roles, Long storeId, Long tenantId) {

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

    public boolean isPlatformAdmin() {
        return roles.contains(Roles.PLATFORM_ADMIN);
    }
}
