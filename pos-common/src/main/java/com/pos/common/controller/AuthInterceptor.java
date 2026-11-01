package com.pos.common.controller;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.pos.common.error.ApiException;
import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.CurrentUser;
import com.pos.common.security.InvalidTokenException;
import com.pos.common.security.ParsedToken;
import com.pos.common.security.TokenRevocations;
import com.pos.common.security.JwtService;
import com.pos.common.security.RequireRole;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Validates the bearer token of every {@code /api/**} request (except the configured public paths),
 * exposes the user through {@link CurrentUser} and enforces {@link RequireRole}.
 */
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final TokenRevocations revocations;

    public AuthInterceptor(JwtService jwtService) {
        this(jwtService, TokenRevocations.NONE);
    }

    public AuthInterceptor(JwtService jwtService, TokenRevocations revocations) {
        this.jwtService = jwtService;
        this.revocations = revocations;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw ApiException.unauthorized("Missing bearer token");
        }
        String token = header.substring(7).trim();
        try {
            ParsedToken parsed = jwtService.parseDetailed(token);
            if (revocations.isRevoked(parsed)) {
                throw ApiException.unauthorized("Token has been revoked, please sign in again");
            }
            AuthenticatedUser user = parsed.user();
            CurrentUser.set(user, token);
            if (handler instanceof HandlerMethod handlerMethod) {
                RequireRole required = handlerMethod.getMethodAnnotation(RequireRole.class);
                if (required == null) {
                    required = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
                }
                if (required != null) {
                    CurrentUser.requireAnyRole(required.value());
                }
            }
            return true;
        } catch (InvalidTokenException e) {
            CurrentUser.clear();
            throw ApiException.unauthorized(e.getMessage());
        } catch (RuntimeException e) {
            CurrentUser.clear();
            throw e;
        }
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        CurrentUser.clear();
    }
}
