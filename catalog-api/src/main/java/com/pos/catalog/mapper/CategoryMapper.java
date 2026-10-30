package com.pos.catalog.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.catalog.domain.Category;

@Mapper
public interface CategoryMapper {

    @Insert("INSERT INTO categories (name, parent_id, active) VALUES (#{name}, #{parentId}, #{active})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Category category);

    @Select("SELECT * FROM categories WHERE id = #{id}")
    Category findById(@Param("id") Long id);

    @Select("SELECT * FROM categories ORDER BY name")
    List<Category> findAll();

    @Update("UPDATE categories SET name = #{name}, parent_id = #{parentId} WHERE id = #{id}")
    int update(Category category);

    @Update("UPDATE categories SET active = #{active} WHERE id = #{id}")
    int setActive(@Param("id") Long id, @Param("active") boolean active);
}
