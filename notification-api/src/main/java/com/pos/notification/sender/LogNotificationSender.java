package com.pos.notification.sender;

import org.springframework.stereotype.Component;

import com.pos.notification.dto.Notification;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LogNotificationSender implements NotificationSender {

    @Override
    public void send(Notification notification) {
        log.info("[{}] to={} subject='{}'\n{}", notification.getChannel(), notification.getRecipient(),
                notification.getSubject(), notification.getBody());
    }
}
