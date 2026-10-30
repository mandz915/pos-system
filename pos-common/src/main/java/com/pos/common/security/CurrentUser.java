package com.pos.common.security;

import java.util.Arrays;
import java.util.Optional;

import com.pos.common.error.ApiException;

/**
 * Holds the authenticated user of the current request (set by the auth interceptor, cleared after the request).
 */
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

    /** The raw bearer token of the current request, used to call other services on behalf of the user. */
    public static String token() {
        return TOKEN.get();
    }

    public static void requireAnyRole(String... roles) {
        if (!get().hasAnyRole(roles)) {
            throw ApiException.forbidden("Requires one of the roles: " + Arrays.toString(roles));
        }
    }

    /**
     * Admins and head-office users (no store bound) may access every store; everybody else only their own.
     * A {@code null} store id is accepted (callers decide what it means).
     */
    public static void requireStoreAccess(Long storeId) {
        AuthenticatedUser user = get();
        if (storeId == null || user.isAdmin() || user.storeId() == null || user.storeId().equals(storeId)) {
            return;
        }
        throw ApiException.forbidden("You do not have access to store " + storeId);
    }

    /**
     * Resolves the store to filter a query by: the requested one (after an access check) or, when none was
     * requested, the user's own store (which is {@code null} for head-office users, meaning "all stores").
     */
    public static Long effectiveStoreId(Long requested) {
        if (requested != null) {
            requireStoreAccess(requested);
            return requested;
        }
        return get().storeId();
    }
}
