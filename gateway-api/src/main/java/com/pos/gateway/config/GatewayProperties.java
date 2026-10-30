package com.pos.gateway.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param allowedOrigins comma separated CORS origins ("*" allows any origin, fine for development only)
 * @param publicPaths    comma separated path patterns that do not need a JWT (login)
 * @param services       base URL of every downstream service, keyed user, inventory, sales, administration, catalog,
 *                       payment, customer, purchasing, reporting, notification
 */
@ConfigurationProperties(prefix = "pos.gateway")
public record GatewayProperties(
        @DefaultValue("*") String allowedOrigins,
        @DefaultValue("/api/auth/login,/api/auth/pin-login") String publicPaths,
        @DefaultValue RateLimit rateLimit,
        Map<String, String> services) {

    public record RateLimit(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("600") int requestsPerMinute,
            /** Only enable behind a trusted load balancer that sets X-Forwarded-For; clients could spoof it otherwise. */
            @DefaultValue("false") boolean trustForwardedFor) {
    }
}
