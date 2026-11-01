package com.pos.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pos.common.error.ApiException;
import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.CurrentUser;
import com.pos.common.security.Roles;
import com.pos.common.security.TokenRevocations;
import com.pos.user.domain.User;
import com.pos.user.mapper.UserMapper;
import com.pos.user.dto.UserDtos;

/** Signing people out when their access changes: the security reason Redis is in this system. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserMapper users;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthService authService;
    @Mock
    private AuditService audit;
    @Mock
    private TokenRevocations revocations;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(users, passwordEncoder, authService, audit, revocations);
        CurrentUser.set(new AuthenticatedUser(1L, "admin", Set.of(Roles.ADMIN), null, 1L), "token");
        User target = new User();
        target.setId(10L);
        target.setStoreId(1L);
        when(users.findById(10L)).thenReturn(target);
        when(passwordEncoder.encode("NewSecret#1")).thenReturn("hash");
    }

    @AfterEach
    void clear() {
        CurrentUser.clear();
    }

    @Test
    void disablingAnAccountRevokesItsTokens() {
        service.setActive(10L, false);

        verify(users).setActive(10L, false);
        verify(revocations).revokeUser(10L);
    }

    @Test
    void enablingAnAccountDoesNotRevokeAnything() {
        service.setActive(10L, true);

        verify(revocations, never()).revokeUser(anyLong());
    }

    @Test
    void anAdminCannotDisableTheirOwnAccount() {
        User self = new User();
        self.setId(1L);
        when(users.findById(1L)).thenReturn(self);

        assertThatThrownBy(() -> service.setActive(1L, false)).isInstanceOf(ApiException.class);

        verify(revocations, never()).revokeUser(anyLong());
    }

    @Test
    void aPasswordResetSignsTheUserOutEverywhere() {
        service.resetPassword(10L, "NewSecret#1");

        verify(users).updatePassword(10L, "hash");
        verify(revocations).revokeUser(10L);
    }

    @Test
    void forcingSignOutRevokesWithoutChangingTheAccount() {
        service.forceSignOut(10L);

        verify(revocations).revokeUser(10L);
        verify(users, never()).setActive(anyLong(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void theRolePickerNeverOffersThePlatformRole() {
        when(users.allRoles()).thenReturn(List.of("PLATFORM_ADMIN", "ADMIN", "MANAGER", "SUPERVISOR", "CASHIER"));

        org.assertj.core.api.Assertions.assertThat(service.roles())
                .containsExactly("ADMIN", "MANAGER", "SUPERVISOR", "CASHIER");
    }

    @Test
    void aCompanyAdministratorCannotCreateAPlatformOperator() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.create(new UserDtos.CreateUserRequest(
                        "sneaky", "Secret#123", "Sneaky", null, null, Set.of(Roles.PLATFORM_ADMIN), null)))
                .isInstanceOf(com.pos.common.error.ApiException.class)
                .hasMessageContaining("Unknown role");
        org.mockito.Mockito.verify(users, org.mockito.Mockito.never()).insert(org.mockito.ArgumentMatchers.any());
    }
}
