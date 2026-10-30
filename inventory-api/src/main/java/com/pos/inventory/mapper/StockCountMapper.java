package com.pos.inventory.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.inventory.domain.StockCount;
import com.pos.inventory.domain.StockCountLine;

@Mapper
public interface StockCountMapper {

    @Insert("INSERT INTO stock_counts (store_id, created_by) VALUES (#{storeId}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(StockCount count);

    @Select("SELECT * FROM stock_counts WHERE id = #{id}")
    StockCount findById(@Param("id") Long id);

    @Select("SELECT * FROM stock_counts WHERE id = #{id} FOR UPDATE")
    StockCount lock(@Param("id") Long id);

    @Select("""
            <script>
            SELECT * FROM stock_counts
            <where>
              <if test="storeId != null">store_id = #{storeId}</if>
            </where>
            ORDER BY id DESC LIMIT 100
            </script>
            """)
    List<StockCount> findAll(@Param("storeId") Long storeId);

    @Update("UPDATE stock_counts SET status = 'POSTED', posted_at = now() WHERE id = #{id}")
    int markPosted(@Param("id") Long id);

    @Insert("""
            INSERT INTO stock_count_lines (count_id, product_id, counted_qty) VALUES (#{countId}, #{productId}, #{countedQty})
            ON CONFLICT (count_id, product_id) DO UPDATE SET counted_qty = EXCLUDED.counted_qty
            """)
    int upsertLine(@Param("countId") Long countId, @Param("productId") Long productId, @Param("countedQty") BigDecimal countedQty);

    @Update("UPDATE stock_count_lines SET system_qty = #{systemQty} WHERE id = #{id}")
    int setSystemQty(@Param("id") Long id, @Param("systemQty") BigDecimal systemQty);

    @Select("SELECT * FROM stock_count_lines WHERE count_id = #{countId} ORDER BY product_id")
    List<StockCountLine> lines(@Param("countId") Long countId);
}
