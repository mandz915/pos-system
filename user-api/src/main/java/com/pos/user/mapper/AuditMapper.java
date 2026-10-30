package com.pos.user.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.pos.user.domain.AuditEntry;

@Mapper
public interface AuditMapper {

    @Insert("""
            INSERT INTO audit_log (user_id, username, action, entity, entity_id, details)
            VALUES (#{userId}, #{username}, #{action}, #{entity}, #{entityId}, #{details})
            """)
    int insert(@Param("userId") Long userId,
               @Param("username") String username,
               @Param("action") String action,
               @Param("entity") String entity,
               @Param("entityId") String entityId,
               @Param("details") String details);

    @Select("""
            <script>
            SELECT * FROM audit_log
            <where>
              <if test="userId != null">AND user_id = #{userId}</if>
              <if test="action != null">AND action = #{action}</if>
            </where>
            ORDER BY id DESC LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<AuditEntry> search(@Param("userId") Long userId,
                            @Param("action") String action,
                            @Param("size") int size,
                            @Param("offset") int offset);

    @Select("""
            <script>
            SELECT count(*) FROM audit_log
            <where>
              <if test="userId != null">AND user_id = #{userId}</if>
              <if test="action != null">AND action = #{action}</if>
            </where>
            </script>
            """)
    long count(@Param("userId") Long userId, @Param("action") String action);
}
