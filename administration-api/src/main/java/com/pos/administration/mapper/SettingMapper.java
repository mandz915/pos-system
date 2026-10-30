package com.pos.administration.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.pos.administration.domain.Setting;

@Mapper
public interface SettingMapper {

    /** Global defaults (store 0) and the overrides of the given store, global first. */
    @Select("SELECT * FROM settings WHERE store_id IN (0, #{storeId}) ORDER BY store_id, setting_key")
    List<Setting> findEffective(@Param("storeId") Long storeId);

    @Insert("""
            INSERT INTO settings (store_id, setting_key, setting_value) VALUES (#{storeId}, #{key}, #{value})
            ON CONFLICT (store_id, setting_key)
            DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = now()
            """)
    int upsert(@Param("storeId") Long storeId, @Param("key") String key, @Param("value") String value);
}
