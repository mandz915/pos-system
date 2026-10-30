package com.pos.inventory.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.pos.inventory.domain.StockMovement;

@Mapper
public interface MovementMapper {

    @Insert("""
            INSERT INTO stock_movements (store_id, product_id, type, quantity, balance_after, ref_type, ref_id, note, created_by)
            VALUES (#{storeId}, #{productId}, #{type}, #{quantity}, #{balanceAfter}, #{refType}, #{refId}, #{note}, #{createdBy})
            """)
    int insert(StockMovement movement);

    @Select("""
            <script>
            SELECT * FROM stock_movements
            <where>
              <if test="storeId != null">AND store_id = #{storeId}</if>
              <if test="productId != null">AND product_id = #{productId}</if>
            </where>
            ORDER BY id DESC LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<StockMovement> search(@Param("storeId") Long storeId,
                               @Param("productId") Long productId,
                               @Param("size") int size,
                               @Param("offset") int offset);

    @Select("""
            <script>
            SELECT count(*) FROM stock_movements
            <where>
              <if test="storeId != null">AND store_id = #{storeId}</if>
              <if test="productId != null">AND product_id = #{productId}</if>
            </where>
            </script>
            """)
    long count(@Param("storeId") Long storeId, @Param("productId") Long productId);
}
