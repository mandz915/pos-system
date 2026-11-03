package com.pos.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pos.common.security.RequireRole;
import com.pos.common.security.Roles;
import com.pos.common.dto.PageResult;
import com.pos.user.dto.AuditEntry;
import com.pos.user.service.AuditService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@RequireRole({Roles.ADMIN, Roles.MANAGER})
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public PageResult<AuditEntry> search(@RequestParam(required = false) Long userId,
                                         @RequestParam(required = false) String action,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer size) {
        return auditService.search(userId, action, page, size);
    }
}
