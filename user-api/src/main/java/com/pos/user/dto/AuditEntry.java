package com.pos.user.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class AuditEntry {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String entity;
    private String entityId;
    private String details;
    private Instant createdAt;
}
