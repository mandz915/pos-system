package com.pos.sales.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.sales.domain.SaleLine;

@Mapper
public interface SaleLineMapper {

    @Insert("""
            INSERT INTO sale_lines (sale_id, line_no, product_id, sku, name, quantity, unit_price, discount_per_unit,
                discount_total, tax_rate, tax_inclusive, tax_amount, line_total)
            VALUES (#{saleId}, #{lineNo}, #{productId}, #{sku}, #{name}, #{quantity}, #{unitPrice}, #{discountPerUnit},
                #{discountTotal}, #{taxRate}, #{taxInclusive}, #{taxAmount}, #{lineTotal})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(SaleLine line);

    @Select("SELECT * FROM sale_lines WHERE sale_id = #{saleId} ORDER BY line_no")
    List<SaleLine> listBySale(@Param("saleId") Long saleId);

    @Delete("DELETE FROM sale_lines WHERE sale_id = #{saleId}")
    int deleteBySale(@Param("saleId") Long saleId);

    @Update("UPDATE sale_lines SET returned_qty = returned_qty + #{quantity} WHERE id = #{id}")
    int addReturned(@Param("id") Long id, @Param("quantity") BigDecimal quantity);

    @Select("SELECT count(*) FROM sale_lines WHERE sale_id = #{saleId} AND returned_qty < quantity")
    long countNotFullyReturned(@Param("saleId") Long saleId);
}
