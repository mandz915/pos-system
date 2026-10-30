package com.pos.notification.domain;

import java.time.Instant;

import lombok.Data;

@Data
public class Notification {
    private Long id;
    private String eventId;
    private String type;
    /** EMAIL or SMS. */
    private String channel = "EMAIL";
    private String recipient;
    private String subject;
    private String body;
    private Long storeId;
    /** PENDING, SENT or FAILED. */
    private String status;
    private int attempts;
    private String lastError;
    private Instant createdAt;
    private Instant sentAt;
}
