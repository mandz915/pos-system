package com.pos.administration.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.administration.dto.Setting;

@Mapper
public interface SettingMapper {

    List<Setting> findEffective(@Param("storeId") Long storeId);

    int upsert(@Param("storeId") Long storeId, @Param("key") String key, @Param("value") String value);
}
