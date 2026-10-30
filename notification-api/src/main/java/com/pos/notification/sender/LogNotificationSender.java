package com.pos.notification.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.pos.notification.domain.Notification;

/** Development sender: writes the message to the log instead of sending it. */
@Component
public class LogNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);

    @Override
    public void send(Notification notification) {
        log.info("[{}] to={} subject='{}'\n{}", notification.getChannel(), notification.getRecipient(),
                notification.getSubject(), notification.getBody());
    }
}
