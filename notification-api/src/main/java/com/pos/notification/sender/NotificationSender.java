package com.pos.notification.sender;

import com.pos.notification.dto.Notification;

public interface NotificationSender {

    void send(Notification notification) throws Exception;
}
