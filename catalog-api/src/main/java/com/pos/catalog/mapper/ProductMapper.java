package com.pos.catalog.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.pos.catalog.domain.Product;

@Mapper
public interface ProductMapper {

    String SELECT_WITH_TAX = """
            SELECT p.*, t.rate AS tax_rate, t.inclusive AS tax_inclusive
            FROM products p LEFT JOIN tax_rates t ON t.id = p.tax_rate_id
            """;

    @Insert("""
            INSERT INTO products (sku, barcode, name, description, category_id, tax_rate_id, uom, price, cost, image_url, active)
            VALUES (#{sku}, #{barcode}, #{name}, #{description}, #{categoryId}, #{taxRateId}, #{uom}, #{price}, #{cost}, #{imageUrl}, #{active})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Product product);

    @Select(SELECT_WITH_TAX + " WHERE p.id = #{id}")
    Product findById(@Param("id") Long id);

    @Select(SELECT_WITH_TAX + " WHERE p.barcode = #{barcode}")
    Product findByBarcode(@Param("barcode") String barcode);

    @Select(SELECT_WITH_TAX + " WHERE p.sku = #{sku}")
    Product findBySku(@Param("sku") String sku);

    @Select("""
            <script>
            SELECT p.*, t.rate AS tax_rate, t.inclusive AS tax_inclusive
            FROM products p LEFT JOIN tax_rates t ON t.id = p.tax_rate_id
            <where>
              <if test="q != null">AND (p.name ILIKE #{q} OR p.sku ILIKE #{q} OR p.barcode ILIKE #{q})</if>
              <if test="categoryId != null">AND p.category_id = #{categoryId}</if>
              <if test="active != null">AND p.active = #{active}</if>
            </where>
            ORDER BY p.name LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Product> search(@Param("q") String q,
                         @Param("categoryId") Long categoryId,
                         @Param("active") Boolean active,
                         @Param("size") int size,
                         @Param("offset") int offset);

    @Select("""
            <script>
            SELECT count(*) FROM products p
            <where>
              <if test="q != null">AND (p.name ILIKE #{q} OR p.sku ILIKE #{q} OR p.barcode ILIKE #{q})</if>
              <if test="categoryId != null">AND p.category_id = #{categoryId}</if>
              <if test="active != null">AND p.active = #{active}</if>
            </where>
            </script>
            """)
    long count(@Param("q") String q, @Param("categoryId") Long categoryId, @Param("active") Boolean active);

    @Update("""
            UPDATE products SET sku = #{sku}, barcode = #{barcode}, name = #{name}, description = #{description},
                category_id = #{categoryId}, tax_rate_id = #{taxRateId}, uom = #{uom}, price = #{price}, cost = #{cost},
                image_url = #{imageUrl}, active = #{active}, updated_at = now()
            WHERE id = #{id}
            """)
    int update(Product product);

    @Update("UPDATE products SET active = #{active}, updated_at = now() WHERE id = #{id}")
    int setActive(@Param("id") Long id, @Param("active") boolean active);
}
