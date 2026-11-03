package com.pos.notification.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class Notification {
    private Long id;
    private String eventId;
    private String type;

    private String channel = "EMAIL";
    private String recipient;
    private String subject;
    private String body;
    private Long storeId;

    private String status;
    private int attempts;
    private String lastError;
    private Instant createdAt;
    private Instant sentAt;
}
