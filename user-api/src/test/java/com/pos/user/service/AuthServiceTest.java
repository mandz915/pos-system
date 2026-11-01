package com.pos.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pos.common.error.ApiException;
import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.CurrentUser;
import com.pos.common.security.JwtProperties;
import com.pos.common.security.JwtService;
import com.pos.common.security.ParsedToken;
import com.pos.common.security.Roles;
import com.pos.common.security.TokenRevocations;
import com.pos.common.tenant.TenantContext;
import com.pos.user.domain.Tenant;
import com.pos.user.domain.User;
import com.pos.user.mapper.TenantMapper;
import com.pos.user.mapper.UserMapper;
import com.pos.user.dto.UserDtos.LoginRequest;
import com.pos.user.dto.UserDtos.LoginResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtService jwt = new JwtService(
            new JwtProperties("0123456789abcdef0123456789abcdef", Duration.ofHours(1), "pos-system"));

    @Mock
    private UserMapper users;
    @Mock
    private TenantMapper tenants;
    @Mock
    private AuditService audit;
    @Mock
    private TokenRevocations revocations;

    private AuthService service;

    private static Tenant tenant(long id, String status) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setCode("ACME");
        t.setName("Acme Retail");
        t.setStatus(status);
        return t;
    }

    @BeforeEach
    void setUp() {
        when(tenants.findByCode("ACME")).thenReturn(tenant(7L, Tenant.ACTIVE));
        service = new AuthService(users, tenants, encoder, jwt, audit, revocations);
    }

    @AfterEach
    void clear() {
        CurrentUser.clear();
    }

    private User user(boolean active) {
        User u = new User();
        u.setId(10L);
        u.setUsername("cora");
        u.setPasswordHash(encoder.encode("Secret#123"));
        u.setStoreId(1L);
        u.setActive(active);
        return u;
    }

    @Test
    void validCredentialsIssueATokenForTheUsersRolesAndStore() {
        when(users.findByUsername("cora")).thenReturn(user(true));
        when(users.rolesOf(10L)).thenReturn(List.of(Roles.CASHIER));

        LoginResponse response = service.login(new LoginRequest("ACME", "cora", "Secret#123"));

        AuthenticatedUser parsed = jwt.parse(response.accessToken());
        assertThat(parsed.userId()).isEqualTo(10L);
        assertThat(parsed.roles()).containsExactly(Roles.CASHIER);
        assertThat(parsed.storeId()).isEqualTo(1L);
        assertThat(parsed.tenantId()).isEqualTo(7L);
        assertThat(response.expiresInSeconds()).isEqualTo(3600);
    }

    @Test
    void theUserIsLookedUpInsideTheCompanyNamedAtLogin() {
        java.util.concurrent.atomic.AtomicReference<Long> seen = new java.util.concurrent.atomic.AtomicReference<>();
        when(users.findByUsername("cora")).thenAnswer(call -> {
            seen.set(TenantContext.currentOrNull());
            return user(true);
        });
        when(users.rolesOf(10L)).thenReturn(List.of(Roles.CASHIER));

        service.login(new LoginRequest("ACME", "cora", "Secret#123"));

        assertThat(seen.get()).isEqualTo(7L);
        assertThat(TenantContext.currentOrNull()).isNull();
    }

    @Test
    void anUnknownCompanyGetsTheSameAnswerAsAWrongPassword() {
        when(tenants.findByCode("NOPE")).thenReturn(null);

        assertThatThrownBy(() -> service.login(new LoginRequest("NOPE", "cora", "Secret#123")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid company, username or password");
        verifyNoInteractions(users);
    }

    @Test
    void aSuspendedCompanyCannotLogIn() {
        when(tenants.findByCode("ACME")).thenReturn(tenant(7L, Tenant.SUSPENDED));

        assertThatThrownBy(() -> service.login(new LoginRequest("ACME", "cora", "Secret#123")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("suspended");
        verifyNoInteractions(users);
    }

    @Test
    void aBlankCompanyMeansAPlatformOperatorAndGetsTenantZero() {
        User platform = user(true);
        platform.setStoreId(null);
        when(users.findByUsername("cora")).thenReturn(platform);
        when(users.rolesOf(10L)).thenReturn(List.of(Roles.PLATFORM_ADMIN));

        LoginResponse response = service.login(new LoginRequest(null, "cora", "Secret#123"));

        AuthenticatedUser parsed = jwt.parse(response.accessToken());
        assertThat(parsed.tenantId()).isZero();
        assertThat(parsed.isPlatformAdmin()).isTrue();
        verifyNoInteractions(tenants);
    }

    @Test
    void aWrongPasswordIsAuditedAndRefused() {
        when(users.findByUsername("cora")).thenReturn(user(true));

        assertThatThrownBy(() -> service.login(new LoginRequest("ACME", "cora", "wrong")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid company, username or password");

        verify(audit).recordFor(eq(null), eq("cora"), eq("LOGIN_FAILED"), eq("user"), eq("cora"), eq(null));
    }

    @Test
    void anUnknownUserGetsTheSameMessageAsAWrongPassword() {
        when(users.findByUsername("ghost")).thenReturn(null);

        assertThatThrownBy(() -> service.login(new LoginRequest("ACME", "ghost", "x")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid company, username or password");
    }

    @Test
    void aDisabledAccountCannotLogInEvenWithTheRightPassword() {
        when(users.findByUsername("cora")).thenReturn(user(false));

        assertThatThrownBy(() -> service.login(new LoginRequest("ACME", "cora", "Secret#123")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void logoutRevokesExactlyThePresentedToken() {
        String token = jwt.issue(new AuthenticatedUser(10L, "cora", Set.of(Roles.CASHIER), 1L, 1L));
        CurrentUser.set(jwt.parse(token), token);

        service.logout();

        ArgumentCaptor<ParsedToken> captor = ArgumentCaptor.forClass(ParsedToken.class);
        verify(revocations).revokeToken(captor.capture());
        assertThat(captor.getValue().jti()).isEqualTo(jwt.parseDetailed(token).jti());
        verify(revocations, never()).revokeUser(anyLong());
    }
}
