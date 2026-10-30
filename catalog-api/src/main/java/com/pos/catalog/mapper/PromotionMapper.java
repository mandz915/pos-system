package com.pos.catalog.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.catalog.domain.Promotion;

@Mapper
public interface PromotionMapper {

    @Insert("""
            INSERT INTO promotions (code, name, type, value, product_id, category_id, starts_at, ends_at, active)
            VALUES (#{code}, #{name}, #{type}, #{value}, #{productId}, #{categoryId}, #{startsAt}, #{endsAt}, #{active})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Promotion promotion);

    @Select("SELECT * FROM promotions WHERE id = #{id}")
    Promotion findById(@Param("id") Long id);

    @Select("SELECT * FROM promotions ORDER BY id DESC")
    List<Promotion> findAll();

    /** Promotions that are switched on and inside their validity window right now. */
    @Select("""
            SELECT * FROM promotions
            WHERE active = TRUE
              AND (starts_at IS NULL OR starts_at <= now())
              AND (ends_at IS NULL OR ends_at > now())
            """)
    List<Promotion> findLive();

    @Update("""
            UPDATE promotions SET code = #{code}, name = #{name}, type = #{type}, value = #{value},
                product_id = #{productId}, category_id = #{categoryId}, starts_at = #{startsAt},
                ends_at = #{endsAt}, active = #{active}
            WHERE id = #{id}
            """)
    int update(Promotion promotion);
}
