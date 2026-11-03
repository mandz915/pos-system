package com.pos.common.tenant;

import java.util.function.Supplier;

import com.pos.common.error.ApiException;
import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.CurrentUser;

public final class TenantContext {

    public static final long PLATFORM = 0L;

    private static final ThreadLocal<Long> OVERRIDE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SYSTEM = new ThreadLocal<>();

    private TenantContext() {
    }

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

    public static boolean isSystem() {
        return Boolean.TRUE.equals(SYSTEM.get());
    }

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
