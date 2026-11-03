package com.pos.administration.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.administration.dto.Terminal;

@Mapper
public interface TerminalMapper {

    int insert(Terminal terminal);

    Terminal findById(@Param("id") Long id);

    List<Terminal> findAll(@Param("storeId") Long storeId);

    int update(Terminal terminal);

    int setActive(@Param("id") Long id, @Param("active") boolean active);

    int heartbeat(@Param("id") Long id);
}
