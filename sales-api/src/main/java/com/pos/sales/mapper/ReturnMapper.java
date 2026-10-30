package com.pos.sales.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.pos.sales.domain.SaleReturn;
import com.pos.sales.domain.SaleReturnLine;

@Mapper
public interface ReturnMapper {

    @Insert("""
            INSERT INTO sale_returns (sale_id, shift_id, refund_total, method, reason, restock, created_by)
            VALUES (#{saleId}, #{shiftId}, #{refundTotal}, #{method}, #{reason}, #{restock}, #{createdBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(SaleReturn saleReturn);

    @Insert("""
            INSERT INTO sale_return_lines (return_id, sale_line_id, product_id, quantity, amount)
            VALUES (#{returnId}, #{saleLineId}, #{productId}, #{quantity}, #{amount})
            """)
    int insertLine(SaleReturnLine line);

    @Select("SELECT * FROM sale_returns WHERE sale_id = #{saleId} ORDER BY id")
    List<SaleReturn> listBySale(@Param("saleId") Long saleId);
}
