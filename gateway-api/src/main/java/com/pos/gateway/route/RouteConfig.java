package com.pos.gateway.route;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import com.pos.gateway.config.GatewayProperties;

/** Maps the public {@code /api/**} paths to the services that own them. */
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class RouteConfig {

    /** service key (see {@link GatewayProperties#services()}) to the path prefixes it owns. */
    static final String[][] ROUTES = {
            {"user", "/api/auth/**", "/api/users/**", "/api/roles/**", "/api/audit/**"},
            {"inventory", "/api/inventory/**"},
            {"sales", "/api/sales/**"},
            {"administration", "/api/admin/**"},
            {"catalog", "/api/catalog/**"},
            {"payment", "/api/payments/**"},
            {"customer", "/api/customers/**"},
            {"purchasing", "/api/purchasing/**"},
            {"reporting", "/api/reports/**"},
            {"notification", "/api/notifications/**"},
    };

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder, GatewayProperties properties) {
        RouteLocatorBuilder.Builder routes = builder.routes();
        for (String[] route : ROUTES) {
            String service = route[0];
            String uri = properties.services() == null ? null : properties.services().get(service);
            if (uri == null || uri.isBlank()) {
                throw new IllegalStateException("Missing pos.gateway.services." + service);
            }
            String[] paths = Arrays.copyOfRange(route, 1, route.length);
            routes.route(service + "-api", r -> r.path(paths).uri(uri));
        }
        return routes.build();
    }

    @Bean
    public CorsWebFilter corsWebFilter(GatewayProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        List<String> origins = Arrays.stream(properties.allowedOrigins().split(",")).map(String::trim)
                .filter(o -> !o.isEmpty()).toList();
        cors.setAllowedOriginPatterns(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setExposedHeaders(List.of("Content-Disposition", "Retry-After"));
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return new CorsWebFilter(source);
    }
}
