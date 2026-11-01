package com.pos.notification.sender;

import org.springframework.stereotype.Component;

import com.pos.notification.domain.Notification;

import lombok.extern.slf4j.Slf4j;

/** Development sender: writes the message to the log instead of sending it. */
@Component
@Slf4j
public class LogNotificationSender implements NotificationSender {


    @Override
    public void send(Notification notification) {
        log.info("[{}] to={} subject='{}'\n{}", notification.getChannel(), notification.getRecipient(),
                notification.getSubject(), notification.getBody());
    }
}
