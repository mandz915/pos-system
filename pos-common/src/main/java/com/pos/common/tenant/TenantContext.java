package com.pos.common.tenant;

import java.util.function.Supplier;

import com.pos.common.error.ApiException;
import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.CurrentUser;

/**
 * The tenant (client company) the current thread works for.
 *
 * <p>For a web request it is the tenant in the JWT. Code that runs without a signed-in user (Kafka consumers, login,
 * start-up work) sets it explicitly with {@link #callAs(Long, Supplier)}. The database connection wrapper copies
 * it into the PostgreSQL session, where row-level security makes every other tenant's rows invisible, so a query that
 * forgets a tenant filter still cannot see another client's data. No tenant at all means no rows.
 */
public final class TenantContext {

    /** Tenant id of the platform operators (users with the PLATFORM_ADMIN role). Owns no business data. */
    public static final long PLATFORM = 0L;

    private static final ThreadLocal<Long> OVERRIDE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SYSTEM = new ThreadLocal<>();

    private TenantContext() {
    }

    /** The tenant of this thread, or {@code null} when there is none. */
    public static Long currentOrNull() {
        Long explicit = OVERRIDE.get();
        if (explicit != null) {
            return explicit;
        }
        return CurrentUser.find().map(AuthenticatedUser::tenantId).orElse(null);
    }

    public static Long require() {
        Long tenant = currentOrNull();
        if (tenant == null) {
            throw ApiException.unauthorized("No tenant in this context");
        }
        return tenant;
    }

    /** True inside {@link #callAsSystem(Supplier)}: background work that must see every tenant's rows. */
    public static boolean isSystem() {
        return Boolean.TRUE.equals(SYSTEM.get());
    }

    /** Sets the tenant for the rest of this thread's current unit of work. Pair with {@link #clear()}. */
    public static void set(Long tenantId) {
        if (tenantId == null) {
            OVERRIDE.remove();
        } else {
            OVERRIDE.set(tenantId);
        }
    }

    public static void clear() {
        OVERRIDE.remove();
        SYSTEM.remove();
    }

    public static <T> T callAs(Long tenantId, Supplier<T> work) {
        Long previous = OVERRIDE.get();
        Boolean previousSystem = SYSTEM.get();
        try {
            set(tenantId);
            SYSTEM.remove();
            return work.get();
        } finally {
            restore(previous, previousSystem);
        }
    }

    public static void runAs(Long tenantId, Runnable work) {
        callAs(tenantId, () -> {
            work.run();
            return null;
        });
    }

    /**
     * Runs cross-tenant background work (for example sending every client's queued notifications). Only the tables
     * that explicitly allow it (their policy checks the system flag) honour this; all others stay tenant-scoped.
     */
    public static <T> T callAsSystem(Supplier<T> work) {
        Long previous = OVERRIDE.get();
        Boolean previousSystem = SYSTEM.get();
        try {
            OVERRIDE.remove();
            SYSTEM.set(Boolean.TRUE);
            return work.get();
        } finally {
            restore(previous, previousSystem);
        }
    }

    private static void restore(Long previous, Boolean previousSystem) {
        if (previous == null) {
            OVERRIDE.remove();
        } else {
            OVERRIDE.set(previous);
        }
        if (previousSystem == null) {
            SYSTEM.remove();
        } else {
            SYSTEM.set(previousSystem);
        }
    }
}
