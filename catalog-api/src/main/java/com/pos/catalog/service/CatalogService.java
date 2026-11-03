package com.pos.catalog.service;

import java.util.List;

import com.pos.catalog.dto.Category;
import com.pos.catalog.dto.Product;
import com.pos.catalog.dto.Promotion;
import com.pos.catalog.dto.TaxRate;
import com.pos.catalog.dto.CatalogDtos.CategoryRequest;
import com.pos.catalog.dto.CatalogDtos.ProductRequest;
import com.pos.catalog.dto.CatalogDtos.ProductView;
import com.pos.catalog.dto.CatalogDtos.PromotionRequest;
import com.pos.catalog.dto.CatalogDtos.TaxRateRequest;
import com.pos.common.dto.PageResult;

public interface CatalogService {

    PageResult<ProductView> searchProducts(String q, Long categoryId, Boolean active, Integer page, Integer size);

    ProductView getProduct(Long id);

    ProductView getByBarcode(String barcode);

    ProductView getBySku(String sku);

    ProductView createProduct(ProductRequest request);

    ProductView updateProduct(Long id, ProductRequest request);

    ProductView setProductActive(Long id, boolean active);

    List<Category> listCategories();

    Category createCategory(CategoryRequest request);

    Category updateCategory(Long id, CategoryRequest request);

    Category setCategoryActive(Long id, boolean active);

    List<TaxRate> listTaxRates();

    TaxRate createTaxRate(TaxRateRequest request);

    TaxRate updateTaxRate(Long id, TaxRateRequest request);

    List<Promotion> listPromotions(boolean liveOnly);

    Promotion createPromotion(PromotionRequest request);

    Promotion updatePromotion(Long id, PromotionRequest request);
}
