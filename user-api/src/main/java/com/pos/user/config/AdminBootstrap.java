package com.pos.user.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pos.common.security.Roles;
import com.pos.user.domain.User;
import com.pos.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

/** Creates the first administrator when the user table is empty, so the system can be logged into. */
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserMapper users;
    private final PasswordEncoder passwordEncoder;

    @Value("${pos.bootstrap.admin-username:admin}")
    private String adminUsername;

    @Value("${pos.bootstrap.admin-password:Admin#12345}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (users.countAll() > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setFullName("System Administrator");
        users.insert(admin);
        users.addRole(admin.getId(), Roles.ADMIN);
        log.warn("Created bootstrap administrator '{}'. Change its password immediately.", adminUsername);
    }
}
