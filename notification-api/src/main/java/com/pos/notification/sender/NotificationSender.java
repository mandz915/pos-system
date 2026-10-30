package com.pos.notification.sender;

import com.pos.notification.domain.Notification;

/**
 * Delivers one notification. The default implementation only logs; add a bean that talks to your email/SMS provider
 * (SMTP, SES, SendGrid, Semaphore, Twilio...) and remove {@link LogNotificationSender} to go live.
 * Throw an exception to signal a failure: the dispatcher retries and eventually marks the notification FAILED.
 */
public interface NotificationSender {

    void send(Notification notification) throws Exception;
}
