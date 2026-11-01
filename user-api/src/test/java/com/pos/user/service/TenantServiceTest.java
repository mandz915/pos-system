package com.pos.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.pos.common.error.ApiException;
import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.CurrentUser;
import com.pos.common.security.Roles;
import com.pos.common.security.TokenRevocations;
import com.pos.common.tenant.TenantContext;
import com.pos.user.domain.Tenant;
import com.pos.user.domain.User;
import com.pos.user.mapper.TenantMapper;
import com.pos.user.mapper.UserMapper;
import com.pos.user.dto.TenantDtos.CreateTenantRequest;
import com.pos.user.dto.TenantDtos.TenantView;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantServiceTest {

    @Mock
    private TenantMapper tenants;
    @Mock
    private UserMapper users;
    @Mock
    private TokenRevocations revocations;
    @Mock
    private AuditService audit;
    @Mock
    private PlatformTransactionManager txManager;

    private TenantService service;

    private static final CreateTenantRequest REQUEST =
            new CreateTenantRequest("acme", "Acme Retail", "boss", "Secret#123", null);

    @BeforeEach
    void setUp() {
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new TenantService(tenants, users, new BCryptPasswordEncoder(4), revocations, audit, txManager);
        CurrentUser.set(new AuthenticatedUser(100L, "platform", Set.of(Roles.PLATFORM_ADMIN), null, 0L), "token");
        doAnswer(call -> {
            Tenant t = call.getArgument(0);
            t.setId(2L);
            return 1;
        }).when(tenants).insert(any());
        Tenant stored = new Tenant();
        stored.setId(2L);
        stored.setCode("ACME");
        stored.setName("Acme Retail");
        stored.setStatus(Tenant.ACTIVE);
        when(tenants.findById(2L)).thenReturn(stored);
    }

    @AfterEach
    void clear() {
        CurrentUser.clear();
        TenantContext.clear();
    }

    @Test
    void creatingACompanyStoresItsCodeUppercasedAndMakesItsFirstAdministratorInsideTheNewTenant() {
        AtomicReference<Long> tenantWhileInserting = new AtomicReference<>();
        doAnswer(call -> {
            User u = call.getArgument(0);
            u.setId(55L);
            tenantWhileInserting.set(TenantContext.currentOrNull());
            return 1;
        }).when(users).insert(any());

        TenantView view = service.create(REQUEST);

        assertThat(view.code()).isEqualTo("ACME");
        assertThat(tenantWhileInserting.get()).isEqualTo(2L);
        verify(users).addRole(55L, Roles.ADMIN);
        verify(audit).record("TENANT_CREATED", "tenant", "2", "code=ACME");
    }

    @Test
    void aDuplicateCodeIsRefused() {
        when(tenants.findByCode("ACME")).thenReturn(new Tenant());

        assertThatThrownBy(() -> service.create(REQUEST)).isInstanceOf(ApiException.class)
                .hasMessageContaining("already in use");
        verify(tenants, never()).insert(any());
    }

    @Test
    void ifTheFirstAdministratorCannotBeCreatedTheCompanyIsRemovedAgain() {
        doAnswer(call -> {
            throw new IllegalStateException("db error");
        }).when(users).insert(any());

        assertThatThrownBy(() -> service.create(REQUEST)).isInstanceOf(IllegalStateException.class);

        verify(tenants).delete(2L);
    }

    @Test
    void suspendingSignsEveryUserOfThatCompanyOutAndOnlyThatCompany() {
        AtomicReference<Long> tenantWhileListing = new AtomicReference<>();
        when(users.allIds()).thenAnswer(call -> {
            tenantWhileListing.set(TenantContext.currentOrNull());
            return List.of(10L, 11L);
        });

        service.suspend(2L);

        verify(tenants).setStatus(2L, Tenant.SUSPENDED);
        assertThat(tenantWhileListing.get()).isEqualTo(2L);
        verify(revocations).revokeUser(10L);
        verify(revocations).revokeUser(11L);
    }

    @Test
    void anUnknownCompanyIsNotFound() {
        assertThatThrownBy(() -> service.get(404L)).isInstanceOf(ApiException.class);
    }
}
