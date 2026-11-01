package com.pos.common.tenant;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Refuses to start when the application's database role would bypass row-level security (a superuser or a role with
 * BYPASSRLS), because tenant isolation would then silently not exist. Migrations run under a separate owner role
 * (spring.flyway.user), so the application role can and must be a plain one.
 */
@Slf4j
@RequiredArgsConstructor
public class TenantIsolationGuard implements ApplicationRunner, Ordered {

    private final JdbcTemplate jdbc;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        Boolean bypass = jdbc.queryForObject(
                "SELECT rolsuper OR rolbypassrls FROM pg_roles WHERE rolname = current_user", Boolean.class);
        if (Boolean.TRUE.equals(bypass)) {
            throw new IllegalStateException("The application connects to PostgreSQL as a superuser or BYPASSRLS role, "
                    + "so tenant isolation (row-level security) would not apply. Connect as a plain role "
                    + "(DB_USER, see scripts/init-databases.sh) and keep the owner role for Flyway "
                    + "(FLYWAY_USER), or set pos.tenancy.enforce-rls=false for local experiments only.");
        }
        log.info("Tenant isolation active: the database role is subject to row-level security");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
