package com.pos.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private static JwtService service(String secret, Duration ttl) {
        return new JwtService(new JwtProperties(secret, ttl, "pos-system"));
    }

    @Test
    void issuesAndParsesToken() {
        JwtService jwt = service(SECRET, Duration.ofHours(1));
        String token = jwt.issue(new AuthenticatedUser(7L, "cashier1", Set.of(Roles.CASHIER), 3L));

        AuthenticatedUser parsed = jwt.parse(token);

        assertThat(parsed.userId()).isEqualTo(7L);
        assertThat(parsed.username()).isEqualTo("cashier1");
        assertThat(parsed.roles()).containsExactly(Roles.CASHIER);
        assertThat(parsed.storeId()).isEqualTo(3L);
    }

    @Test
    void headOfficeUserHasNoStore() {
        JwtService jwt = service(SECRET, Duration.ofHours(1));
        String token = jwt.issue(new AuthenticatedUser(1L, "admin", Set.of(Roles.ADMIN), null));

        assertThat(jwt.parse(token).storeId()).isNull();
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        String token = service(SECRET, Duration.ofHours(1))
                .issue(new AuthenticatedUser(1L, "u", Set.of(Roles.ADMIN), null));

        JwtService other = service("ffffffffffffffffffffffffffffffff", Duration.ofHours(1));

        assertThatThrownBy(() -> other.parse(token)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService jwt = service(SECRET, Duration.ofSeconds(-30));
        String token = jwt.issue(new AuthenticatedUser(1L, "u", Set.of(Roles.ADMIN), null));

        assertThatThrownBy(() -> jwt.parse(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void rejectsGarbage() {
        JwtService jwt = service(SECRET, Duration.ofHours(1));

        assertThatThrownBy(() -> jwt.parse("not-a-jwt")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> service("too-short", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }
}
