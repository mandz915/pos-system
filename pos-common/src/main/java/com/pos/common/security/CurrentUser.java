package com.pos.common.security;

import java.util.Arrays;
import java.util.Optional;

import com.pos.common.error.ApiException;

public final class CurrentUser {

    private static final ThreadLocal<AuthenticatedUser> USER = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void set(AuthenticatedUser user, String token) {
        USER.set(user);
        TOKEN.set(token);
    }

    public static void clear() {
        USER.remove();
        TOKEN.remove();
    }

    public static AuthenticatedUser get() {
        AuthenticatedUser user = USER.get();
        if (user == null) {
            throw ApiException.unauthorized("Authentication required");
        }
        return user;
    }

    public static Optional<AuthenticatedUser> find() {
        return Optional.ofNullable(USER.get());
    }

    public static String token() {
        return TOKEN.get();
    }

    public static void requireAnyRole(String... roles) {
        if (!get().hasAnyRole(roles)) {
            throw ApiException.forbidden("Requires one of the roles: " + Arrays.toString(roles));
        }
    }

    public static void requireStoreAccess(Long storeId) {
        AuthenticatedUser user = get();
        if (storeId == null || user.isAdmin() || user.storeId() == null || user.storeId().equals(storeId)) {
            return;
        }
        throw ApiException.forbidden("You do not have access to store " + storeId);
    }

    public static Long effectiveStoreId(Long requested) {
        if (requested != null) {
            requireStoreAccess(requested);
            return requested;
        }
        return get().storeId();
    }
}
