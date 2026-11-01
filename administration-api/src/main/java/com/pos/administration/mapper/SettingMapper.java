package com.pos.administration.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.administration.domain.Setting;

@Mapper
public interface SettingMapper {

    /** Global defaults (store 0) and the overrides of the given store, global first. */
    List<Setting> findEffective(@Param("storeId") Long storeId);

    void upsert(@Param("storeId") Long storeId, @Param("key") String key, @Param("value") String value);
}
