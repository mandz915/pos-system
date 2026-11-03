package com.pos.user.service;

import java.util.List;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pos.common.error.ApiException;
import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.CurrentUser;
import com.pos.common.security.Roles;
import com.pos.common.security.TokenRevocations;
import com.pos.common.util.AfterCommit;
import com.pos.common.dto.PageResult;
import com.pos.common.dto.Paging;
import com.pos.user.dto.User;
import com.pos.user.mapper.UserMapper;
import com.pos.user.dto.UserDtos.ChangePasswordRequest;
import com.pos.user.dto.UserDtos.CreateUserRequest;
import com.pos.user.dto.UserDtos.UpdateUserRequest;
import com.pos.user.dto.UserDtos.UserView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private static final Set<String> VALID_ROLES = Set.of(Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR, Roles.CASHIER);

    private final UserMapper users;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuditService audit;
    private final TokenRevocations revocations;

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserView> list(String q, Integer page, Integer size) {
        Paging paging = Paging.of(page, size);
        String pattern = q == null || q.isBlank() ? null : "%" + q.trim() + "%";
        List<UserView> items = users.search(pattern, paging.size(), paging.offset()).stream()
                .map(authService::toView).toList();
        return PageResult.of(items, paging, users.count(pattern));
    }

    @Override
    @Transactional(readOnly = true)
    public UserView get(Long id) {
        return authService.toView(require(id));
    }

    @Override
    public UserView create(CreateUserRequest request) {
        validateRoles(request.roles());
        assertCanManage(request.storeId());
        if (users.findByUsername(request.username()) != null) {
            throw ApiException.conflict("Username '" + request.username() + "' is already taken");
        }
        User user = new User();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPinHash(request.pin() == null ? null : passwordEncoder.encode(request.pin()));
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email());
        user.setStoreId(request.storeId());
        users.insert(user);
        request.roles().forEach(role -> users.addRole(user.getId(), role));
        audit.record("USER_CREATED", "user", String.valueOf(user.getId()), "roles=" + request.roles());
        return authService.toView(require(user.getId()));
    }

    @Override
    public UserView update(Long id, UpdateUserRequest request) {
        User user = require(id);
        validateRoles(request.roles());
        assertCanManage(user.getStoreId());
        assertCanManage(request.storeId());
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email());
        user.setStoreId(request.storeId());
        users.update(user);
        users.clearRoles(id);
        request.roles().forEach(role -> users.addRole(id, role));
        audit.record("USER_UPDATED", "user", String.valueOf(id), "roles=" + request.roles());
        revokeSessions(id);
        return authService.toView(require(id));
    }

    @Override
    public UserView setActive(Long id, boolean active) {
        User user = require(id);
        assertCanManage(user.getStoreId());
        if (!active && CurrentUser.get().userId().equals(id)) {
            throw ApiException.badRequest("You cannot disable your own account");
        }
        users.setActive(id, active);
        audit.record(active ? "USER_ENABLED" : "USER_DISABLED", "user", String.valueOf(id), null);
        if (!active) {
            revokeSessions(id);
        }
        return authService.toView(require(id));
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        User user = require(id);
        assertCanManage(user.getStoreId());
        users.updatePassword(id, passwordEncoder.encode(newPassword));
        audit.record("PASSWORD_RESET", "user", String.valueOf(id), null);
        revokeSessions(id);
    }

    @Override
    public void setPin(Long id, String pin) {
        User user = require(id);
        assertCanManage(user.getStoreId());
        users.updatePin(id, passwordEncoder.encode(pin));
        audit.record("PIN_CHANGED", "user", String.valueOf(id), null);
    }

    @Override
    public void changeOwnPassword(ChangePasswordRequest request) {
        Long id = CurrentUser.get().userId();
        User user = require(id);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        users.updatePassword(id, passwordEncoder.encode(request.newPassword()));
        audit.record("PASSWORD_CHANGED", "user", String.valueOf(id), null);
        revokeSessions(id);
    }

    @Override
    public void forceSignOut(Long id) {
        User user = require(id);
        assertCanManage(user.getStoreId());
        audit.record("SESSIONS_REVOKED", "user", String.valueOf(id), null);
        revokeSessions(id);
    }

    private void revokeSessions(Long id) {

        AfterCommit.run(() -> revocations.revokeUser(id));
    }

    @Override
    public List<String> roles() {

        return users.allRoles().stream().filter(VALID_ROLES::contains).toList();
    }

    private User require(Long id) {
        User user = users.findById(id);
        if (user == null) {
            throw ApiException.notFound("User", id);
        }
        return user;
    }

    private void validateRoles(Set<String> roles) {
        for (String role : roles) {
            if (!VALID_ROLES.contains(role)) {
                throw ApiException.badRequest("Unknown role: " + role);
            }
        }
        if (roles.contains(Roles.ADMIN) && !CurrentUser.get().isAdmin()) {
            throw ApiException.forbidden("Only an administrator can grant the ADMIN role");
        }
    }

    private void assertCanManage(Long targetStoreId) {
        AuthenticatedUser actor = CurrentUser.get();
        if (actor.isAdmin() || actor.storeId() == null) {
            return;
        }
        if (targetStoreId == null || !targetStoreId.equals(actor.storeId())) {
            throw ApiException.forbidden("You can only manage users of your own store");
        }
    }
}
