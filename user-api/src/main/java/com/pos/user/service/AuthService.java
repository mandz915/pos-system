package com.pos.user.service;

import java.util.List;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.pos.common.error.ApiException;
import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.CurrentUser;
import com.pos.common.security.JwtService;
import com.pos.common.security.TokenRevocations;
import com.pos.common.tenant.TenantContext;
import com.pos.user.domain.Tenant;
import com.pos.user.domain.User;
import com.pos.user.mapper.TenantMapper;
import com.pos.user.mapper.UserMapper;
import com.pos.user.dto.UserDtos.LoginRequest;
import com.pos.user.dto.UserDtos.LoginResponse;
import com.pos.user.dto.UserDtos.PinLoginRequest;
import com.pos.user.dto.UserDtos.UserView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper users;
    private final TenantMapper tenants;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService audit;
    private final TokenRevocations revocations;

    public LoginResponse login(LoginRequest request) {
        Long tenantId = resolveTenant(request.tenant());
        return TenantContext.callAs(tenantId, () -> {
            User user = users.findByUsername(request.username());
            if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                audit.recordFor(null, request.username(), "LOGIN_FAILED", "user", request.username(), null);
                throw ApiException.unauthorized("Invalid company, username or password");
            }
            return issueToken(user, tenantId, "LOGIN");
        });
    }

    public LoginResponse pinLogin(PinLoginRequest request) {
        Long tenantId = resolveTenant(request.tenant());
        return TenantContext.callAs(tenantId, () -> {
            User user = users.findByUsername(request.username());
            if (user == null || user.getPinHash() == null
                    || !passwordEncoder.matches(request.pin(), user.getPinHash())) {
                audit.recordFor(null, request.username(), "PIN_LOGIN_FAILED", "user", request.username(), null);
                throw ApiException.unauthorized("Invalid company, username or PIN");
            }
            return issueToken(user, tenantId, "PIN_LOGIN");
        });
    }

    /**
     * The company code selects the tenant whose users are searched; a blank code means a platform operator.
     * An unknown code gets the same answer as a wrong password, so codes cannot be probed.
     */
    private Long resolveTenant(String code) {
        if (code == null || code.isBlank()) {
            return TenantContext.PLATFORM;
        }
        Tenant tenant = tenants.findByCode(code.trim());
        if (tenant == null) {
            throw ApiException.unauthorized("Invalid company, username or password");
        }
        if (!tenant.isActive()) {
            throw ApiException.forbidden("This company's account is suspended");
        }
        return tenant.getId();
    }

    /** Revokes the token of the current request so it cannot be used again, even before it expires. */
    public void logout() {
        revocations.revokeToken(jwtService.parseDetailed(CurrentUser.token()));
        AuthenticatedUser user = CurrentUser.get();
        audit.recordFor(user.userId(), user.username(), "LOGOUT", "user", String.valueOf(user.userId()), null);
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

    private LoginResponse issueToken(User user, Long tenantId, String auditAction) {
        if (!user.isActive()) {
            throw ApiException.forbidden("This account is disabled");
        }
        List<String> roles = users.rolesOf(user.getId());
        String token = jwtService.issue(
                new AuthenticatedUser(user.getId(), user.getUsername(), Set.copyOf(roles), user.getStoreId(),
                        tenantId));
        audit.recordFor(user.getId(), user.getUsername(), auditAction, "user", String.valueOf(user.getId()), null);
        return new LoginResponse(token, "Bearer", jwtService.getTtl().toSeconds(), toView(user));
    }
}
