package com.pos.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;

class RevocationKeysTest {

    private static final Instant ISSUED = Instant.parse("2026-06-01T10:00:00.500Z");
    private final ParsedToken token = new ParsedToken(
            new AuthenticatedUser(7L, "cora", Set.of(Roles.CASHIER), 1L, 1L), "jti-1", ISSUED, ISSUED.plusSeconds(3600));

    @Test
    void notRevokedWhenNothingIsStored() {
        assertThat(RevocationKeys.isRevoked(token, null, null)).isFalse();
    }

    @Test
    void aSingleRevokedTokenIsDetected() {
        assertThat(RevocationKeys.isRevoked(token, "1", null)).isTrue();
    }

    @Test
    void userRevocationCoversTokensIssuedBeforeOrAtThatMoment() {
        assertThat(RevocationKeys.isRevoked(token, null, String.valueOf(ISSUED.toEpochMilli() + 1))).isTrue();
        assertThat(RevocationKeys.isRevoked(token, null, String.valueOf(ISSUED.toEpochMilli()))).isTrue();
    }

    @Test
    void aTokenIssuedAfterTheRevocationIsStillValid() {
        assertThat(RevocationKeys.isRevoked(token, null, String.valueOf(ISSUED.toEpochMilli() - 1))).isFalse();
    }

    @Test
    void anUnreadableMarkerFailsSafe() {
        assertThat(RevocationKeys.isRevoked(token, null, "garbage")).isTrue();
    }
}
