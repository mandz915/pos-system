package com.pos.catalog.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pos.catalog.dto.Product;

@Mapper
public interface ProductMapper {

    int insert(Product product);

    Product findById(@Param("id") Long id);

    Product findByBarcode(@Param("barcode") String barcode);

    Product findBySku(@Param("sku") String sku);

    List<Product> search(@Param("q") String q,
                         @Param("categoryId") Long categoryId,
                         @Param("active") Boolean active,
                         @Param("size") int size,
                         @Param("offset") int offset);

    long count(@Param("q") String q, @Param("categoryId") Long categoryId, @Param("active") Boolean active);

    int update(Product product);

    int setActive(@Param("id") Long id, @Param("active") boolean active);
}
