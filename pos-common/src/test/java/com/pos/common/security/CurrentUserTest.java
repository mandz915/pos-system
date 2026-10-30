package com.pos.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.pos.common.error.ApiException;

class CurrentUserTest {

    @AfterEach
    void cleanUp() {
        CurrentUser.clear();
    }

    @Test
    void cashierCanOnlyAccessOwnStore() {
        CurrentUser.set(new AuthenticatedUser(1L, "c", Set.of(Roles.CASHIER), 5L), "t");

        CurrentUser.requireStoreAccess(5L);
        assertThatThrownBy(() -> CurrentUser.requireStoreAccess(6L)).isInstanceOf(ApiException.class);
    }

    @Test
    void adminAndHeadOfficeAccessAnyStore() {
        CurrentUser.set(new AuthenticatedUser(1L, "a", Set.of(Roles.ADMIN), 5L), "t");
        CurrentUser.requireStoreAccess(99L);

        CurrentUser.set(new AuthenticatedUser(2L, "m", Set.of(Roles.MANAGER), null), "t");
        CurrentUser.requireStoreAccess(99L);
    }

    @Test
    void effectiveStoreDefaultsToUsersStore() {
        CurrentUser.set(new AuthenticatedUser(1L, "c", Set.of(Roles.CASHIER), 5L), "t");

        assertThat(CurrentUser.effectiveStoreId(null)).isEqualTo(5L);
        assertThat(CurrentUser.effectiveStoreId(5L)).isEqualTo(5L);
    }

    @Test
    void roleCheck() {
        CurrentUser.set(new AuthenticatedUser(1L, "c", Set.of(Roles.CASHIER), 5L), "t");

        CurrentUser.requireAnyRole(Roles.CASHIER, Roles.MANAGER);
        assertThatThrownBy(() -> CurrentUser.requireAnyRole(Roles.ADMIN)).isInstanceOf(ApiException.class);
    }

    @Test
    void unauthenticatedAccessFails() {
        assertThatThrownBy(CurrentUser::get).isInstanceOf(ApiException.class);
    }
}
