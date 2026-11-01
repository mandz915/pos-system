package com.pos.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.pos.common.error.ApiException;
import com.pos.common.security.ParsedToken;
import com.pos.common.security.TokenRevocations;
import com.pos.test.ApiTestSupport;
import com.pos.user.service.AuthService;
import com.pos.user.dto.UserDtos.LoginResponse;
import com.pos.user.dto.UserDtos.UserView;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerTest {

    private static final String[] PUBLIC = {"/api/auth/login", "/api/auth/pin-login"};

    @Mock
    private AuthService authService;

    @Mock
    private TokenRevocations revocations;

    private MockMvc mvc;

    private static UserView user() {
        return new UserView(10L, "cora", "Cora Cashier", null, 1L, true, List.of("CASHIER"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    @BeforeEach
    void setUp() {
        mvc = ApiTestSupport.mockMvcWithPublicPaths(PUBLIC, new AuthController(authService));
    }

    @Test
    void loginReturnsTheTokenWithoutAnyAuthorizationHeader() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("jwt-value", "Bearer", 28800, user()));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cora\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("cora"));
    }

    @Test
    void theCompanyCodeIsPassedToTheService() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("jwt-value", "Bearer", 28800, user()));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenant\":\"ACME\",\"username\":\"cora\",\"password\":\"secret\"}"))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<com.pos.user.dto.UserDtos.LoginRequest> captor =
                org.mockito.ArgumentCaptor.forClass(com.pos.user.dto.UserDtos.LoginRequest.class);
        org.mockito.Mockito.verify(authService).login(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().tenant()).isEqualTo("ACME");
    }

    @Test
    void wrongCredentialsAreReportedAs401WithTheSharedErrorBody() throws Exception {
        when(authService.login(any())).thenThrow(ApiException.unauthorized("Invalid username or password"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cora\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void aBlankUsernameIsRejectedBeforeTheServiceIsCalled() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verifyNoInteractions(authService);
    }

    @Test
    void meNeedsAToken() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        verifyNoInteractions(authService);
    }

    @Test
    void meReturnsTheProfileOfTheTokenOwner() throws Exception {
        when(authService.me()).thenReturn(user());

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.cashier()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void aTamperedTokenIsRejected() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.cashier() + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutReturns204AndRevokesThroughTheService() throws Exception {
        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.cashier()))
                .andExpect(status().isNoContent());

        verify(authService).logout();
    }

    @Test
    void aRevokedTokenIsRefusedEvenThoughItIsStillSigned() throws Exception {
        when(revocations.isRevoked(any(ParsedToken.class))).thenReturn(true);
        MockMvc revoking = ApiTestSupport.mockMvcWithRevocations(revocations, new AuthController(authService));

        revoking.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.cashier()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token has been revoked, please sign in again"));

        verifyNoInteractions(authService);
    }
}
