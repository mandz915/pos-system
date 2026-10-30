package com.pos.inventory.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.inventory.domain.StockLevel;

@Mapper
public interface StockLevelMapper {

    @Insert("INSERT INTO stock_levels (store_id, product_id) VALUES (#{storeId}, #{productId}) ON CONFLICT (store_id, product_id) DO NOTHING")
    int ensure(@Param("storeId") Long storeId, @Param("productId") Long productId);

    @Select("SELECT * FROM stock_levels WHERE store_id = #{storeId} AND product_id = #{productId} FOR UPDATE")
    StockLevel lock(@Param("storeId") Long storeId, @Param("productId") Long productId);

    @Select("SELECT * FROM stock_levels WHERE store_id = #{storeId} AND product_id = #{productId}")
    StockLevel find(@Param("storeId") Long storeId, @Param("productId") Long productId);

    @Update("UPDATE stock_levels SET quantity = #{quantity}, updated_at = now() WHERE id = #{id}")
    int updateQuantity(@Param("id") Long id, @Param("quantity") BigDecimal quantity);

    @Update("UPDATE stock_levels SET reorder_level = #{level}, updated_at = now() WHERE store_id = #{storeId} AND product_id = #{productId}")
    int updateReorderLevel(@Param("storeId") Long storeId, @Param("productId") Long productId, @Param("level") BigDecimal level);

    @Select("""
            <script>
            SELECT * FROM stock_levels
            <where>
              <if test="storeId != null">AND store_id = #{storeId}</if>
              <if test="productId != null">AND product_id = #{productId}</if>
              <if test="lowOnly">AND reorder_level &gt; 0 AND quantity &lt;= reorder_level</if>
            </where>
            ORDER BY store_id, product_id LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<StockLevel> search(@Param("storeId") Long storeId,
                            @Param("productId") Long productId,
                            @Param("lowOnly") boolean lowOnly,
                            @Param("size") int size,
                            @Param("offset") int offset);

    @Select("""
            <script>
            SELECT count(*) FROM stock_levels
            <where>
              <if test="storeId != null">AND store_id = #{storeId}</if>
              <if test="productId != null">AND product_id = #{productId}</if>
              <if test="lowOnly">AND reorder_level &gt; 0 AND quantity &lt;= reorder_level</if>
            </where>
            </script>
            """)
    long count(@Param("storeId") Long storeId, @Param("productId") Long productId, @Param("lowOnly") boolean lowOnly);
}
