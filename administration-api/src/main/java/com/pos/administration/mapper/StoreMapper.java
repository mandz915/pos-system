package com.pos.administration.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.administration.domain.Store;

@Mapper
public interface StoreMapper {

    @Insert("INSERT INTO stores (code, name, address, phone, tin, active) VALUES (#{code}, #{name}, #{address}, #{phone}, #{tin}, #{active})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Store store);

    @Select("SELECT * FROM stores WHERE id = #{id}")
    Store findById(@Param("id") Long id);

    @Select("SELECT * FROM stores ORDER BY name")
    List<Store> findAll();

    @Update("UPDATE stores SET code = #{code}, name = #{name}, address = #{address}, phone = #{phone}, tin = #{tin} WHERE id = #{id}")
    int update(Store store);

    @Update("UPDATE stores SET active = #{active} WHERE id = #{id}")
    int setActive(@Param("id") Long id, @Param("active") boolean active);
}
