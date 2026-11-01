package com.pos.common.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.pos.common.error.ApiException;
import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.CurrentUser;
import com.pos.common.security.Roles;

class TenantContextTest {

    @AfterEach
    void clear() {
        TenantContext.clear();
        CurrentUser.clear();
    }

    @Test
    void thereIsNoTenantByDefaultAndRequireFails() {
        assertThat(TenantContext.currentOrNull()).isNull();
        assertThatThrownBy(TenantContext::require).isInstanceOf(ApiException.class);
    }

    @Test
    void theTenantComesFromTheSignedInUser() {
        CurrentUser.set(new AuthenticatedUser(1L, "a", Set.of(Roles.ADMIN), null, 7L), "t");

        assertThat(TenantContext.require()).isEqualTo(7L);
    }

    @Test
    void anExplicitTenantWinsAndIsRestoredAfterwards() {
        CurrentUser.set(new AuthenticatedUser(1L, "a", Set.of(Roles.ADMIN), null, 7L), "t");

        Long inside = TenantContext.callAs(9L, TenantContext::require);

        assertThat(inside).isEqualTo(9L);
        assertThat(TenantContext.require()).isEqualTo(7L);
    }

    @Test
    void systemModeHasNoTenantAndIsLeftAgain() {
        TenantContext.callAsSystem(() -> {
            assertThat(TenantContext.isSystem()).isTrue();
            assertThat(TenantContext.currentOrNull()).isNull();
            return null;
        });

        assertThat(TenantContext.isSystem()).isFalse();
    }

    @Test
    void enteringATenantLeavesSystemModeSoItCannotLeakIntoTenantWork() {
        TenantContext.callAsSystem(() -> TenantContext.callAs(3L, () -> {
            assertThat(TenantContext.isSystem()).isFalse();
            return null;
        }));
    }

    @Test
    void platformOperatorsHaveTheirOwnTenantThatOwnsNoData() {
        CurrentUser.set(new AuthenticatedUser(1L, "p", Set.of(Roles.PLATFORM_ADMIN), null, TenantContext.PLATFORM), "t");

        assertThat(TenantContext.require()).isZero();
        assertThat(CurrentUser.get().isPlatformAdmin()).isTrue();
        assertThat(CurrentUser.get().isAdmin()).isFalse();
    }
}
