package com.pos.user.service;

import com.pos.common.dto.PageResult;
import com.pos.user.dto.TenantDtos.CreateTenantRequest;
import com.pos.user.dto.TenantDtos.TenantView;

public interface TenantService {

    TenantView create(CreateTenantRequest request);

    PageResult<TenantView> list(String q, Integer page, Integer size);

    TenantView get(Long id);

    TenantView rename(Long id, String name);

    TenantView suspend(Long id);

    TenantView activate(Long id);
}
