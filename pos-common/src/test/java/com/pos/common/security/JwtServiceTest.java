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
        String token = jwt.issue(new AuthenticatedUser(7L, "cashier1", Set.of(Roles.CASHIER), 3L, 1L));

        AuthenticatedUser parsed = jwt.parse(token);

        assertThat(parsed.userId()).isEqualTo(7L);
        assertThat(parsed.username()).isEqualTo("cashier1");
        assertThat(parsed.roles()).containsExactly(Roles.CASHIER);
        assertThat(parsed.storeId()).isEqualTo(3L);
    }

    @Test
    void headOfficeUserHasNoStore() {
        JwtService jwt = service(SECRET, Duration.ofHours(1));
        String token = jwt.issue(new AuthenticatedUser(1L, "admin", Set.of(Roles.ADMIN), null, 1L));

        assertThat(jwt.parse(token).storeId()).isNull();
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        String token = service(SECRET, Duration.ofHours(1))
                .issue(new AuthenticatedUser(1L, "u", Set.of(Roles.ADMIN), null, 1L));

        JwtService other = service("ffffffffffffffffffffffffffffffff", Duration.ofHours(1));

        assertThatThrownBy(() -> other.parse(token)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService jwt = service(SECRET, Duration.ofSeconds(-30));
        String token = jwt.issue(new AuthenticatedUser(1L, "u", Set.of(Roles.ADMIN), null, 1L));

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

    @Test
    void everyTokenHasItsOwnIdAndRevocationTimestamps() {
        JwtService jwt = service(SECRET, Duration.ofHours(1));
        AuthenticatedUser user = new AuthenticatedUser(7L, "cashier1", Set.of(Roles.CASHIER), 3L, 1L);
        long before = System.currentTimeMillis();

        ParsedToken first = jwt.parseDetailed(jwt.issue(user));
        ParsedToken second = jwt.parseDetailed(jwt.issue(user));

        assertThat(first.jti()).isNotBlank().isNotEqualTo(second.jti());
        assertThat(first.issuedAt().toEpochMilli()).isBetween(before, System.currentTimeMillis());
        assertThat(first.expiresAt()).isAfter(first.issuedAt());
        assertThat(first.user().userId()).isEqualTo(7L);
    }

    @Test
    void theTenantIsCarriedInTheToken() {
        JwtService jwt = service(SECRET, Duration.ofHours(1));

        AuthenticatedUser parsed = jwt.parse(jwt.issue(new AuthenticatedUser(7L, "c", Set.of(Roles.CASHIER), 3L, 42L)));

        assertThat(parsed.tenantId()).isEqualTo(42L);
    }

    @Test
    void platformOperatorsCarryTenantZero() {
        JwtService jwt = service(SECRET, Duration.ofHours(1));

        AuthenticatedUser parsed = jwt.parse(jwt.issue(
                new AuthenticatedUser(1L, "p", Set.of(Roles.PLATFORM_ADMIN), null, 0L)));

        assertThat(parsed.tenantId()).isZero();
        assertThat(parsed.isPlatformAdmin()).isTrue();
    }

    @Test
    void aTokenWithoutATenantIsRejectedEvenWhenProperlySigned() throws Exception {
        com.nimbusds.jwt.JWTClaimsSet claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
                .issuer("pos-system").subject("old").claim("uid", 1L).claim("roles", java.util.List.of("ADMIN"))
                .expirationTime(java.util.Date.from(java.time.Instant.now().plusSeconds(600))).build();
        com.nimbusds.jwt.SignedJWT jwt = new com.nimbusds.jwt.SignedJWT(
                new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256), claims);
        jwt.sign(new com.nimbusds.jose.crypto.MACSigner(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> service(SECRET, Duration.ofHours(1)).parse(jwt.serialize()))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("tenant");
    }
}
