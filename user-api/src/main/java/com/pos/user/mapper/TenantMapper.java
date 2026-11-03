package com.pos.user.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.user.dto.Tenant;

@Mapper
public interface TenantMapper {

    int insert(Tenant tenant);

    Tenant findById(@Param("id") Long id);

    Tenant findByCode(@Param("code") String code);

    List<Tenant> search(@Param("q") String q, @Param("size") int size, @Param("offset") int offset);

    long count(@Param("q") String q);

    int updateName(@Param("id") Long id, @Param("name") String name);

    int setStatus(@Param("id") Long id, @Param("status") String status);

    int delete(@Param("id") Long id);
}
