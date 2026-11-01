package com.pos.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

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

import com.pos.test.ApiTestSupport;
import com.pos.user.service.TenantService;
import com.pos.user.dto.TenantDtos.TenantView;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantControllerTest {

    @Mock
    private TenantService tenantService;

    private MockMvc mvc;

    private static final String BODY = """
            {"code":"acme","name":"Acme Retail","adminUsername":"boss","adminPassword":"Secret#123"}""";

    @BeforeEach
    void setUp() {
        mvc = ApiTestSupport.mockMvc(new TenantController(tenantService));
    }

    private static TenantView acme() {
        return new TenantView(2L, "ACME", "Acme Retail", "ACTIVE", Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void aPlatformOperatorCreatesACompanyWithItsFirstAdministrator() throws Exception {
        when(tenantService.create(any())).thenReturn(acme());

        mvc.perform(post("/api/platform/tenants").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ACME"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void aCompanyAdministratorCannotManageCompanies() throws Exception {
        mvc.perform(post("/api/platform/tenants").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.admin())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/platform/tenants").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.manager()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(tenantService);
    }

    @Test
    void anonymousCallersAreRefused() throws Exception {
        mvc.perform(get("/api/platform/tenants")).andExpect(status().isUnauthorized());
    }

    @Test
    void theCompanyCodeAndPasswordAreValidated() throws Exception {
        mvc.perform(post("/api/platform/tenants").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"bad code!","name":"X","adminUsername":"boss","adminPassword":"short"}"""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tenantService);
    }

    @Test
    void suspendingACompanyIsAPlatformAction() throws Exception {
        when(tenantService.suspend(2L)).thenReturn(new TenantView(2L, "ACME", "Acme Retail", "SUSPENDED", null));

        mvc.perform(post("/api/platform/tenants/2/suspend")
                        .header(HttpHeaders.AUTHORIZATION, ApiTestSupport.platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
        mvc.perform(post("/api/platform/tenants/2/suspend").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.adminOfOtherCompany()))
                .andExpect(status().isForbidden());
    }
}
