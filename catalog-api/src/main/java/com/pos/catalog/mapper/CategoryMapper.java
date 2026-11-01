package com.pos.catalog.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.catalog.domain.Category;

@Mapper
public interface CategoryMapper {

    int insert(Category category);

    Category findById(@Param("id") Long id);

    List<Category> findAll();

    int update(Category category);

    void setActive(@Param("id") Long id, @Param("active") boolean active);
}
