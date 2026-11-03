package com.pos.notification.service;

import com.pos.common.dto.PageResult;
import com.pos.notification.dto.Notification;

public interface NotificationService {

    void queue(String eventId, String type, Long storeId, NotificationTemplates.Message message);

    PageResult<Notification> search(Long storeId, String status, Integer page, Integer size);

    Notification retry(Long id);
}
