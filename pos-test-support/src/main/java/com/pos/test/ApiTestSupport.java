package com.pos.test;

import java.time.Duration;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;

import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.JwtProperties;
import com.pos.common.security.JwtService;
import com.pos.common.security.Roles;
import com.pos.common.security.TokenRevocations;
import com.pos.common.controller.AuthInterceptor;
import com.pos.common.controller.GlobalExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Builds a {@link MockMvc} around ONE controller whose collaborators are Mockito mocks, but with the real pieces of
 * the platform in front of it: the JWT {@link AuthInterceptor} (so {@code @RequireRole} and 401/403 are tested for
 * real, with genuinely signed tokens) and the shared {@link GlobalExceptionHandler} (so error bodies are the real ones).
 * No Spring context is started, so these tests run in milliseconds.
 */
public final class ApiTestSupport {

    public static final String SECRET = "test-secret-test-secret-test-secret-1234";

    private static final JwtService JWT = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1), "pos-system"));

    private ApiTestSupport() {
    }

    public static JwtService jwt() {
        return JWT;
    }

    /** A signed token for a user; {@code storeId == null} means head office (all stores). */
    public static String token(long userId, String username, Long storeId, String... roles) {
        return JWT.issue(new AuthenticatedUser(userId, username, Set.of(roles), storeId, 1L));
    }

    /** A signed token of a user of another company (tenant). */
    public static String tokenOfTenant(long tenantId, long userId, String username, Long storeId, String... roles) {
        return JWT.issue(new AuthenticatedUser(userId, username, Set.of(roles), storeId, tenantId));
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Cashier 10 of store 1. */
    public static String cashier() {
        return bearer(token(10L, "cora", 1L, Roles.CASHIER));
    }

    /** Supervisor 11 of store 1. */
    public static String supervisor() {
        return bearer(token(11L, "sam", 1L, Roles.SUPERVISOR));
    }

    /** Manager 12 of store 1. */
    public static String manager() {
        return bearer(token(12L, "mia", 1L, Roles.MANAGER));
    }

    /** Head-office administrator 1. */
    public static String admin() {
        return bearer(token(1L, "admin", null, Roles.ADMIN));
    }

    /** Platform operator 100 (tenant 0): creates and suspends client companies, owns no business data. */
    public static String platformAdmin() {
        return bearer(tokenOfTenant(0L, 100L, "platform", null, Roles.PLATFORM_ADMIN));
    }

    /** Administrator 200 of company 2 (not company 1, which every other helper uses). */
    public static String adminOfOtherCompany() {
        return bearer(tokenOfTenant(2L, 200L, "boss", null, Roles.ADMIN));
    }

    public static MockMvc mockMvc(Object... controllers) {
        return mockMvc(TokenRevocations.NONE, new String[0], controllers);
    }

    /** Same, but the given paths need no token (login endpoints). */
    public static MockMvc mockMvcWithPublicPaths(String[] publicPaths, Object... controllers) {
        return mockMvc(TokenRevocations.NONE, publicPaths, controllers);
    }

    public static MockMvc mockMvcWithRevocations(TokenRevocations revocations, Object... controllers) {
        return mockMvc(revocations, new String[0], controllers);
    }

    private static MockMvc mockMvc(TokenRevocations revocations, String[] publicPaths, Object[] controllers) {
        AuthInterceptor auth = new AuthInterceptor(JWT, revocations);
        Set<String> open = Set.of(publicPaths);
        HandlerInterceptor guarded = new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
                return open.contains(request.getRequestURI()) || auth.preHandle(request, response, handler);
            }

            @Override
            public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler,
                                        Exception ex) {
                auth.afterCompletion(request, response, handler, ex);
            }
        };
        return MockMvcBuilders.standaloneSetup(controllers)
                .addInterceptors(guarded)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
