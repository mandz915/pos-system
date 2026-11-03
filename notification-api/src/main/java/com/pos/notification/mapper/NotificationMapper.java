package com.pos.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.notification.dto.Notification;

@Mapper
public interface NotificationMapper {

    int insert(Notification notification);

    List<Notification> claimPending(@Param("limit") int limit);

    int markSent(@Param("id") Long id);

    int markAttemptFailed(@Param("id") Long id, @Param("error") String error, @Param("maxAttempts") int maxAttempts);

    int requeue(@Param("id") Long id);

    Notification findById(@Param("id") Long id);

    List<Notification> search(@Param("storeId") Long storeId, @Param("status") String status,
                              @Param("size") int size, @Param("offset") int offset);

    long count(@Param("storeId") Long storeId, @Param("status") String status);
}
