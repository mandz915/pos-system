package com.pos.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.notification.domain.Notification;

@Mapper
public interface NotificationMapper {

    /** @return 1 when stored, 0 when a notification for this event already exists (duplicate delivery). */
    @Insert("""
            INSERT INTO notifications (event_id, type, channel, recipient, subject, body, store_id)
            VALUES (#{eventId}, #{type}, #{channel}, #{recipient}, #{subject}, #{body}, #{storeId})
            ON CONFLICT (event_id) DO NOTHING
            """)
    int insert(Notification notification);

    /** Claims a batch for dispatching; rows are locked so several instances never send the same one twice. */
    @Select("SELECT * FROM notifications WHERE status = 'PENDING' ORDER BY id LIMIT #{limit} FOR UPDATE SKIP LOCKED")
    List<Notification> claimPending(@Param("limit") int limit);

    @Update("UPDATE notifications SET status = 'SENT', attempts = attempts + 1, sent_at = now(), last_error = NULL WHERE id = #{id}")
    int markSent(@Param("id") Long id);

    /** Stays PENDING (retried on the next tick) until {@code maxAttempts} is reached, then FAILED. */
    @Update("""
            UPDATE notifications
            SET attempts = attempts + 1,
                last_error = #{error},
                status = CASE WHEN attempts + 1 >= #{maxAttempts} THEN 'FAILED' ELSE 'PENDING' END
            WHERE id = #{id}
            """)
    int markAttemptFailed(@Param("id") Long id, @Param("error") String error, @Param("maxAttempts") int maxAttempts);

    @Update("UPDATE notifications SET status = 'PENDING', attempts = 0, last_error = NULL WHERE id = #{id} AND status = 'FAILED'")
    int requeue(@Param("id") Long id);

    @Select("SELECT * FROM notifications WHERE id = #{id}")
    Notification findById(@Param("id") Long id);

    @Select("""
            <script>
            SELECT * FROM notifications
            <where>
              <if test="storeId != null">AND store_id = #{storeId}</if>
              <if test="status != null">AND status = #{status}</if>
            </where>
            ORDER BY id DESC LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Notification> search(@Param("storeId") Long storeId, @Param("status") String status,
                              @Param("size") int size, @Param("offset") int offset);

    @Select("""
            <script>
            SELECT count(*) FROM notifications
            <where>
              <if test="storeId != null">AND store_id = #{storeId}</if>
              <if test="status != null">AND status = #{status}</if>
            </where>
            </script>
            """)
    long count(@Param("storeId") Long storeId, @Param("status") String status);
}
