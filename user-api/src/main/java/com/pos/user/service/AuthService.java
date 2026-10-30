package com.pos.user.service;

import java.util.List;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.pos.common.error.ApiException;
import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.CurrentUser;
import com.pos.common.security.JwtService;
import com.pos.user.domain.User;
import com.pos.user.mapper.UserMapper;
import com.pos.user.web.UserDtos.LoginRequest;
import com.pos.user.web.UserDtos.LoginResponse;
import com.pos.user.web.UserDtos.PinLoginRequest;
import com.pos.user.web.UserDtos.UserView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService audit;

    public LoginResponse login(LoginRequest request) {
        User user = users.findByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            audit.recordFor(null, request.username(), "LOGIN_FAILED", "user", request.username(), null);
            throw ApiException.unauthorized("Invalid username or password");
        }
        return issueToken(user, "LOGIN");
    }

    public LoginResponse pinLogin(PinLoginRequest request) {
        User user = users.findByUsername(request.username());
        if (user == null || user.getPinHash() == null || !passwordEncoder.matches(request.pin(), user.getPinHash())) {
            audit.recordFor(null, request.username(), "PIN_LOGIN_FAILED", "user", request.username(), null);
            throw ApiException.unauthorized("Invalid username or PIN");
        }
        return issueToken(user, "PIN_LOGIN");
    }

    public UserView me() {
        User user = users.findById(CurrentUser.get().userId());
        if (user == null) {
            throw ApiException.unauthorized("User no longer exists");
        }
        return toView(user);
    }

    public UserView toView(User user) {
        List<String> roles = users.rolesOf(user.getId());
        return new UserView(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getStoreId(), user.isActive(), roles, user.getCreatedAt());
    }

    private LoginResponse issueToken(User user, String auditAction) {
        if (!user.isActive()) {
            throw ApiException.forbidden("This account is disabled");
        }
        List<String> roles = users.rolesOf(user.getId());
        String token = jwtService.issue(
                new AuthenticatedUser(user.getId(), user.getUsername(), Set.copyOf(roles), user.getStoreId()));
        audit.recordFor(user.getId(), user.getUsername(), auditAction, "user", String.valueOf(user.getId()), null);
        return new LoginResponse(token, "Bearer", jwtService.getTtl().toSeconds(), toView(user));
    }
}
