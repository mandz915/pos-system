package com.pos.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PublicPathsTest {

    private final PublicPaths paths = new PublicPaths("/api/auth/login, /api/auth/pin-login");

    @Test
    void loginEndpointsArePublic() {
        assertThat(paths.matches("/api/auth/login")).isTrue();
        assertThat(paths.matches("/api/auth/pin-login")).isTrue();
    }

    @Test
    void everythingElseNeedsAToken() {
        assertThat(paths.matches("/api/auth/me")).isFalse();
        assertThat(paths.matches("/api/users")).isFalse();
        assertThat(paths.matches("/api/auth/login/extra")).isFalse();
    }

    @Test
    void emptyConfigurationMakesNothingPublic() {
        assertThat(new PublicPaths("").matches("/api/auth/login")).isFalse();
    }
}
