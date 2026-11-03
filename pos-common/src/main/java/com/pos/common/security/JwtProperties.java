package com.pos.common.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "pos.security.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("8h") Duration ttl,
        @DefaultValue("pos-system") String issuer) {
}
