package com.pos.user.config;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.pos.common.security.Roles;
import com.pos.common.tenant.TenantContext;
import com.pos.user.domain.User;
import com.pos.user.mapper.UserMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Creates the first logins on an empty system so it can be used at all:
 * <ul>
 *   <li>a platform operator ({@code PLATFORM_ADMIN}, no company) who creates the client companies, and</li>
 *   <li>an administrator for the demo company {@code DEFAULT} (tenant 1, created by the migration), which also owns
 *       the seed data of the other services.</li>
 * </ul>
 * Each runs in its own tenant context and transaction, because a connection takes its tenant when it is handed out.
 */
@Component
@Slf4j
public class AdminBootstrap implements CommandLineRunner {

    private final UserMapper users;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate tx;

    @Value("${pos.bootstrap.platform-username:platform}")
    private String platformUsername;

    @Value("${pos.bootstrap.platform-password:Platform#12345}")
    private String platformPassword;

    @Value("${pos.bootstrap.admin-username:admin}")
    private String adminUsername;

    @Value("${pos.bootstrap.admin-password:Admin#12345}")
    private String adminPassword;

    public AdminBootstrap(UserMapper users, PasswordEncoder passwordEncoder, PlatformTransactionManager txManager) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tx = new TransactionTemplate(txManager);
    }

    @Override
    public void run(String @NonNull ... args) {
        createFirstUser(TenantContext.PLATFORM, platformUsername, platformPassword, "Platform Operator",
                Roles.PLATFORM_ADMIN);
        createFirstUser(1L, adminUsername, adminPassword, "System Administrator", Roles.ADMIN);
    }

    private void createFirstUser(Long tenantId, String username, String password, String fullName, String role) {
        TenantContext.runAs(tenantId, () -> tx.executeWithoutResult(status -> {
            if (users.countAll() > 0) {
                return;
            }
            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setFullName(fullName);
            users.insert(user);
            users.addRole(user.getId(), role);
            log.warn("Created bootstrap login '{}' ({}) for tenant {}. Change its password immediately.",
                    username, role, tenantId);
        }));
    }
}
