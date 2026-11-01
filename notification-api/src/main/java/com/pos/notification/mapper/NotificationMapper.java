package com.pos.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.notification.domain.Notification;

@Mapper
public interface NotificationMapper {

    /**
     *
     */
    void insert(Notification notification);

    /** Claims a batch for dispatching; rows are locked so several instances never send the same one twice. */
    List<Notification> claimPending(@Param("limit") int limit);

    void markSent(@Param("id") Long id);

    /**
     * Stays PENDING (retried on the next tick) until {@code maxAttempts} is reached, then FAILED.
     */
    void markAttemptFailed(@Param("id") Long id, @Param("error") String error, @Param("maxAttempts") int maxAttempts);

    int requeue(@Param("id") Long id);

    Notification findById(@Param("id") Long id);

    List<Notification> search(@Param("storeId") Long storeId, @Param("status") String status,
                              @Param("size") int size, @Param("offset") int offset);

    long count(@Param("storeId") Long storeId, @Param("status") String status);
}
