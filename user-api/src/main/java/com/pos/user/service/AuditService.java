package com.pos.user.service;

import com.pos.common.dto.PageResult;
import com.pos.user.dto.AuditEntry;

public interface AuditService {

    void record(String action, String entity, String entityId, String details);

    void recordFor(Long userId, String username, String action, String entity, String entityId, String details);

    PageResult<AuditEntry> search(Long userId, String action, Integer page, Integer size);
}
