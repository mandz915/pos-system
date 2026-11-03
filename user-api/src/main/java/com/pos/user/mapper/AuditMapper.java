package com.pos.user.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.user.dto.AuditEntry;

@Mapper
public interface AuditMapper {

    int insert(@Param("userId") Long userId,
               @Param("username") String username,
               @Param("action") String action,
               @Param("entity") String entity,
               @Param("entityId") String entityId,
               @Param("details") String details);

    List<AuditEntry> search(@Param("userId") Long userId,
                            @Param("action") String action,
                            @Param("size") int size,
                            @Param("offset") int offset);

    long count(@Param("userId") Long userId, @Param("action") String action);
}
