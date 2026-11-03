package com.pos.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pos.common.security.RequireRole;
import com.pos.common.security.Roles;
import com.pos.common.dto.PageResult;
import com.pos.user.service.TenantService;
import com.pos.user.dto.TenantDtos.CreateTenantRequest;
import com.pos.user.dto.TenantDtos.TenantView;
import com.pos.user.dto.TenantDtos.UpdateTenantRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/platform/tenants")
@RequiredArgsConstructor
@RequireRole(Roles.PLATFORM_ADMIN)
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantView create(@Valid @RequestBody CreateTenantRequest request) {
        return tenantService.create(request);
    }

    @GetMapping
    public PageResult<TenantView> list(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer size) {
        return tenantService.list(q, page, size);
    }

    @GetMapping("/{id}")
    public TenantView get(@PathVariable Long id) {
        return tenantService.get(id);
    }

    @PutMapping("/{id}")
    public TenantView rename(@PathVariable Long id, @Valid @RequestBody UpdateTenantRequest request) {
        return tenantService.rename(id, request.name());
    }

    @PostMapping("/{id}/suspend")
    public TenantView suspend(@PathVariable Long id) {
        return tenantService.suspend(id);
    }

    @PostMapping("/{id}/activate")
    public TenantView activate(@PathVariable Long id) {
        return tenantService.activate(id);
    }
}
