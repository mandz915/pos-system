package com.pos.catalog.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.catalog.domain.TaxRate;

@Mapper
public interface TaxRateMapper {

    @Insert("INSERT INTO tax_rates (code, name, rate, inclusive, active) VALUES (#{code}, #{name}, #{rate}, #{inclusive}, #{active})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(TaxRate taxRate);

    @Select("SELECT * FROM tax_rates WHERE id = #{id}")
    TaxRate findById(@Param("id") Long id);

    @Select("SELECT * FROM tax_rates ORDER BY code")
    List<TaxRate> findAll();

    @Update("UPDATE tax_rates SET code = #{code}, name = #{name}, rate = #{rate}, inclusive = #{inclusive}, active = #{active} WHERE id = #{id}")
    int update(TaxRate taxRate);
}
