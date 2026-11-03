package com.pos.user.service;

import org.springframework.stereotype.Service;

import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.CurrentUser;
import com.pos.common.dto.PageResult;
import com.pos.common.dto.Paging;
import com.pos.user.dto.AuditEntry;
import com.pos.user.mapper.AuditMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditMapper audit;

    @Override
    public void record(String action, String entity, String entityId, String details) {
        AuthenticatedUser actor = CurrentUser.find().orElse(null);
        audit.insert(actor == null ? null : actor.userId(), actor == null ? null : actor.username(),
                action, entity, entityId, details);
    }

    @Override
    public void recordFor(Long userId, String username, String action, String entity, String entityId, String details) {
        audit.insert(userId, username, action, entity, entityId, details);
    }

    @Override
    public PageResult<AuditEntry> search(Long userId, String action, Integer page, Integer size) {
        Paging paging = Paging.of(page, size);
        return PageResult.of(audit.search(userId, action, paging.size(), paging.offset()), paging,
                audit.count(userId, action));
    }
}
