package com.pos.administration.domain;

import java.time.Instant;

import lombok.Data;

@Data
public class Terminal {
    private Long id;
    private Long storeId;
    private String code;
    private String name;
    private boolean active = true;
    private Instant lastSeenAt;
    private Instant createdAt;
}
