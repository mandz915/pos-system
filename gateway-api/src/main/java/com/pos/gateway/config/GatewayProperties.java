package com.pos.gateway.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "pos.gateway")
public record GatewayProperties(
        @DefaultValue("*") String allowedOrigins,
        @DefaultValue("/api/auth/login,/api/auth/pin-login") String publicPaths,
        @DefaultValue RateLimit rateLimit,
        Map<String, String> services) {

    public record RateLimit(
            @DefaultValue("true") boolean enabled,

            @DefaultValue("1200") int requestsPerMinute,

            @DefaultValue("60") int anonymousPerMinute,

            @DefaultValue("false") boolean trustForwardedFor) {
    }
}
