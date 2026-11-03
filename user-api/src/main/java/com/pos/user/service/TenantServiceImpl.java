package com.pos.user.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DuplicateKeyException;

import com.pos.common.error.ApiException;
import com.pos.common.security.Roles;
import com.pos.common.security.TokenRevocations;
import com.pos.common.tenant.TenantContext;
import com.pos.common.dto.PageResult;
import com.pos.common.dto.Paging;
import com.pos.user.dto.Tenant;
import com.pos.user.dto.User;
import com.pos.user.mapper.TenantMapper;
import com.pos.user.mapper.UserMapper;
import com.pos.user.dto.TenantDtos.CreateTenantRequest;
import com.pos.user.dto.TenantDtos.TenantView;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TenantServiceImpl implements TenantService {

    private final TenantMapper tenants;
    private final UserMapper users;
    private final PasswordEncoder passwordEncoder;
    private final TokenRevocations revocations;
    private final AuditService audit;
    private final TransactionTemplate tx;

    public TenantServiceImpl(TenantMapper tenants, UserMapper users, PasswordEncoder passwordEncoder,
                         TokenRevocations revocations, AuditService audit, PlatformTransactionManager txManager) {
        this.tenants = tenants;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.revocations = revocations;
        this.audit = audit;
        this.tx = new TransactionTemplate(txManager);
    }

    @Override
    public TenantView create(CreateTenantRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (tenants.findByCode(code) != null) {
            throw ApiException.conflict("Company code '" + code + "' is already in use");
        }
        Tenant tenant = new Tenant();
        tenant.setCode(code);
        tenant.setName(request.name().trim());
        try {
            tenants.insert(tenant);
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("Company code '" + code + "' is already in use");
        }
        try {
            TenantContext.runAs(tenant.getId(), () -> tx.executeWithoutResult(status -> createFirstAdmin(tenant, request)));
        } catch (RuntimeException e) {
            tenants.delete(tenant.getId());
            throw e;
        }
        audit.record("TENANT_CREATED", "tenant", String.valueOf(tenant.getId()), "code=" + code);
        return view(tenants.findById(tenant.getId()));
    }

    private void createFirstAdmin(Tenant tenant, CreateTenantRequest request) {
        User admin = new User();
        admin.setUsername(request.adminUsername().trim());
        admin.setPasswordHash(passwordEncoder.encode(request.adminPassword()));
        admin.setFullName(request.adminFullName() == null || request.adminFullName().isBlank()
                ? tenant.getName() + " Administrator" : request.adminFullName().trim());
        users.insert(admin);
        users.addRole(admin.getId(), Roles.ADMIN);
    }

    @Override
    public PageResult<TenantView> list(String q, Integer page, Integer size) {
        Paging paging = Paging.of(page, size);
        String pattern = q == null || q.isBlank() ? null : "%" + q.trim() + "%";
        List<TenantView> items = tenants.search(pattern, paging.size(), paging.offset()).stream()
                .map(TenantService::view).toList();
        return PageResult.of(items, paging, tenants.count(pattern));
    }

    @Override
    public TenantView get(Long id) {
        return view(require(id));
    }

    @Override
    public TenantView rename(Long id, String name) {
        require(id);
        tenants.updateName(id, name.trim());
        audit.record("TENANT_UPDATED", "tenant", String.valueOf(id), null);
        return view(require(id));
    }

    @Override
    public TenantView suspend(Long id) {
        require(id);
        tenants.setStatus(id, Tenant.SUSPENDED);
        List<Long> userIds = TenantContext.callAs(id, users::allIds);
        userIds.forEach(revocations::revokeUser);
        audit.record("TENANT_SUSPENDED", "tenant", String.valueOf(id), "sessions revoked: " + userIds.size());
        return view(require(id));
    }

    @Override
    public TenantView activate(Long id) {
        require(id);
        tenants.setStatus(id, Tenant.ACTIVE);
        audit.record("TENANT_ACTIVATED", "tenant", String.valueOf(id), null);
        return view(require(id));
    }

    private Tenant require(Long id) {
        Tenant tenant = tenants.findById(id);
        if (tenant == null) {
            throw ApiException.notFound("Company", id);
        }
        return tenant;
    }

    private static TenantView view(Tenant tenant) {
        return new TenantView(tenant.getId(), tenant.getCode(), tenant.getName(), tenant.getStatus(),
                tenant.getCreatedAt());
    }
}
