package com.pos.notification.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PostConstruct;

import com.pos.common.error.ApiException;
import com.pos.common.security.CurrentUser;
import com.pos.common.tenant.TenantContext;
import com.pos.common.dto.PageResult;
import com.pos.common.dto.Paging;
import com.pos.notification.dto.Notification;
import com.pos.notification.mapper.NotificationMapper;
import com.pos.notification.sender.NotificationSender;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BATCH = 50;

    private final NotificationMapper notifications;
    private final NotificationSender sender;
    private final PlatformTransactionManager txManager;
    private TransactionTemplate tx;

    @PostConstruct
    void initTransactionTemplate() {
        this.tx = new TransactionTemplate(txManager);
    }

    @Value("${notification.ops-email:ops@example.com}")
    private String opsEmail;

    @Override
    @Transactional
    public void queue(String eventId, String type, Long storeId, NotificationTemplates.Message message) {
        Notification notification = new Notification();
        notification.setEventId(eventId);
        notification.setType(type);
        notification.setChannel("EMAIL");
        notification.setRecipient(opsEmail);
        notification.setSubject(message.subject());
        notification.setBody(message.body());
        notification.setStoreId(storeId);
        notifications.insert(notification);
    }

    @Scheduled(fixedDelayString = "${notification.dispatch-interval-ms:5000}")
    public void dispatchPending() {

        TenantContext.callAsSystem(() -> {
            tx.executeWithoutResult(status -> dispatchBatch());
            return null;
        });
    }

    private void dispatchBatch() {
        List<Notification> batch = notifications.claimPending(BATCH);
        for (Notification notification : batch) {
            try {
                sender.send(notification);
                notifications.markSent(notification.getId());
            } catch (Exception e) {
                log.warn("Notification {} failed (attempt {}): {}", notification.getId(), notification.getAttempts() + 1,
                        e.getMessage());
                String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                notifications.markAttemptFailed(notification.getId(),
                        error.length() > 250 ? error.substring(0, 250) : error, MAX_ATTEMPTS);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Notification> search(Long storeId, String status, Integer page, Integer size) {
        Long store = CurrentUser.effectiveStoreId(storeId);
        String normalized = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        if (normalized != null && !List.of("PENDING", "SENT", "FAILED").contains(normalized)) {
            throw ApiException.badRequest("Unknown status '" + status + "'");
        }
        Paging paging = Paging.of(page, size);
        return PageResult.of(notifications.search(store, normalized, paging.size(), paging.offset()), paging,
                notifications.count(store, normalized));
    }

    @Override
    @Transactional
    public Notification retry(Long id) {
        Notification notification = notifications.findById(id);
        if (notification == null) {
            throw ApiException.notFound("Notification", id);
        }
        CurrentUser.requireStoreAccess(notification.getStoreId());
        if (notifications.requeue(id) == 0) {
            throw ApiException.conflict("Only a FAILED notification can be retried");
        }
        return notifications.findById(id);
    }
}
