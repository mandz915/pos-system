package com.pos.administration.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.administration.domain.Terminal;

@Mapper
public interface TerminalMapper {

    void insert(Terminal terminal);

    Terminal findById(@Param("id") Long id);

    List<Terminal> findAll(@Param("storeId") Long storeId);

    void update(Terminal terminal);

    void setActive(@Param("id") Long id, @Param("active") boolean active);

    void heartbeat(@Param("id") Long id);
}
