package com.pos.notification.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pos.common.security.RequireRole;
import com.pos.common.security.Roles;
import com.pos.common.web.PageResult;
import com.pos.notification.domain.Notification;
import com.pos.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@RequireRole({Roles.ADMIN, Roles.MANAGER, Roles.SUPERVISOR})
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public PageResult<Notification> list(@RequestParam(required = false) Long storeId,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer size) {
        return service.search(storeId, status, page, size);
    }

    /** Puts a FAILED notification back in the queue. */
    @PostMapping("/{id}/retry")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public Notification retry(@PathVariable Long id) {
        return service.retry(id);
    }
}
