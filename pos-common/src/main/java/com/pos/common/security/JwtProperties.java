package com.pos.common.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * JWT settings shared by every service. All services (and the gateway) must use the same secret.
 *
 * @param secret HMAC-SHA256 secret, at least 32 bytes
 */
@ConfigurationProperties(prefix = "pos.security.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("8h") Duration ttl,
        @DefaultValue("pos-system") String issuer) {
}
