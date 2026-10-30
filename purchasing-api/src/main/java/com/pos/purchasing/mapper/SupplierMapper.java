package com.pos.purchasing.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.purchasing.domain.Supplier;

@Mapper
public interface SupplierMapper {

    @Insert("INSERT INTO suppliers (code, name, contact, phone, email, address, active) VALUES (#{code}, #{name}, #{contact}, #{phone}, #{email}, #{address}, #{active})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Supplier supplier);

    @Select("SELECT * FROM suppliers WHERE id = #{id}")
    Supplier findById(@Param("id") Long id);

    @Select("""
            <script>
            SELECT * FROM suppliers
            <where>
              <if test="q != null">AND (name ILIKE #{q} OR code ILIKE #{q})</if>
            </where>
            ORDER BY name
            </script>
            """)
    List<Supplier> findAll(@Param("q") String q);

    @Update("UPDATE suppliers SET name = #{name}, contact = #{contact}, phone = #{phone}, email = #{email}, address = #{address} WHERE id = #{id}")
    int update(Supplier supplier);

    @Update("UPDATE suppliers SET active = #{active} WHERE id = #{id}")
    int setActive(@Param("id") Long id, @Param("active") boolean active);
}
