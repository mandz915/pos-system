package com.pos.catalog.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pos.catalog.domain.Category;
import com.pos.catalog.domain.Product;
import com.pos.catalog.domain.Promotion;
import com.pos.catalog.domain.TaxRate;
import com.pos.catalog.mapper.CategoryMapper;
import com.pos.catalog.mapper.ProductMapper;
import com.pos.catalog.mapper.PromotionMapper;
import com.pos.catalog.mapper.TaxRateMapper;
import com.pos.catalog.dto.CatalogDtos.CategoryRequest;
import com.pos.catalog.dto.CatalogDtos.ProductRequest;
import com.pos.catalog.dto.CatalogDtos.ProductView;
import com.pos.catalog.dto.CatalogDtos.PromotionRequest;
import com.pos.catalog.dto.CatalogDtos.TaxRateRequest;
import com.pos.catalog.dto.CatalogDtos.TaxInfo;
import com.pos.common.cache.CacheNames;
import com.pos.common.cache.JsonCache;
import com.pos.common.error.ApiException;
import com.pos.common.pricing.PromotionEngine;
import com.pos.common.pricing.PromotionRule;
import com.pos.common.security.CurrentUser;
import com.pos.common.security.Roles;
import com.pos.common.util.AfterCommit;
import com.pos.common.dto.PageResult;
import com.pos.common.dto.Paging;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CatalogService {

    private final ProductMapper products;
    private final CategoryMapper categories;
    private final TaxRateMapper taxRates;
    private final PromotionMapper promotions;
    private final JsonCache cache;

    @Value("${pos.cache.catalog-ttl:30s}")
    private Duration cacheTtl = Duration.ofSeconds(30);

    // ------------------------------------------------------------------ products
    @Transactional(readOnly = true)
    public PageResult<ProductView> searchProducts(String q, Long categoryId, Boolean active, Integer page, Integer size) {
        Paging paging = Paging.of(page, size);
        String pattern = q == null || q.isBlank() ? null : "%" + q.trim() + "%";
        List<PromotionRule> live = liveRules();
        boolean showCost = canSeeCost();
        List<ProductView> items = products.search(pattern, categoryId, active, paging.size(), paging.offset()).stream()
                .map(p -> view(p, live, showCost))
                .toList();
        return PageResult.of(items, paging, products.count(pattern, categoryId, active));
    }

    /**
     * Product lookups are the hottest reads of a POS (every scan), so they are served from Redis when possible.
     * The cache holds the view WITHOUT cost; managers who may see the cost always read the database.
     * No transaction is opened here, so a cache hit does not even take a database connection.
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public ProductView getProduct(Long id) {
        return cached(CacheNames.productById(id), () -> loadProduct(id));
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public ProductView getByBarcode(String barcode) {
        return cached(CacheNames.productByBarcode(barcode), () -> {
            Product product = products.findByBarcode(barcode);
            if (product == null) {
                throw ApiException.notFound("Product with barcode", barcode);
            }
            return view(product, liveRules(), canSeeCost());
        });
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public ProductView getBySku(String sku) {
        return cached(CacheNames.productBySku(sku), () -> {
            Product product = products.findBySku(sku);
            if (product == null) {
                throw ApiException.notFound("Product with SKU", sku);
            }
            return view(product, liveRules(), canSeeCost());
        });
    }

    private ProductView cached(String key, Supplier<ProductView> loader) {
        boolean showCost = canSeeCost();
        if (!showCost) {
            ProductView hit = cache.get(CacheNames.CATALOG, key, ProductView.class).orElse(null);
            if (hit != null) {
                return hit;
            }
        }
        ProductView loaded = loader.get();
        cache.put(CacheNames.CATALOG, key, loaded.withoutCost(), cacheTtl);
        return loaded;
    }

    /** Reads the database, never the cache. Writes use it so they cannot return stale data they just changed. */
    private ProductView loadProduct(Long id) {
        Product product = products.findById(id);
        if (product == null) {
            throw ApiException.notFound("Product", id);
        }
        return view(product, liveRules(), canSeeCost());
    }

    /** Drops cached catalog data once the current transaction has committed. */
    private void evictCatalogCache() {
        AfterCommit.run(() -> cache.evictAll(CacheNames.CATALOG));
    }

    public ProductView createProduct(ProductRequest request) {
        Product product = new Product();
        applyProduct(product, request);
        products.insert(product);
        evictCatalogCache();
        return loadProduct(product.getId());
    }

    public ProductView updateProduct(Long id, ProductRequest request) {
        Product product = products.findById(id);
        if (product == null) {
            throw ApiException.notFound("Product", id);
        }
        applyProduct(product, request);
        products.update(product);
        evictCatalogCache();
        return loadProduct(id);
    }

    public ProductView setProductActive(Long id, boolean active) {
        loadProduct(id);
        products.setActive(id, active);
        evictCatalogCache();
        return loadProduct(id);
    }

    private void applyProduct(Product product, ProductRequest request) {
        if (request.categoryId() != null && categories.findById(request.categoryId()) == null) {
            throw ApiException.badRequest("Unknown category " + request.categoryId());
        }
        if (request.taxRateId() != null && taxRates.findById(request.taxRateId()) == null) {
            throw ApiException.badRequest("Unknown tax rate " + request.taxRateId());
        }
        product.setSku(request.sku().trim());
        product.setBarcode(request.barcode() == null || request.barcode().isBlank() ? null : request.barcode().trim());
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setCategoryId(request.categoryId());
        product.setTaxRateId(request.taxRateId());
        product.setUom(request.uom() == null || request.uom().isBlank() ? "pc" : request.uom().trim());
        product.setPrice(request.price());
        product.setCost(request.cost() == null ? BigDecimal.ZERO : request.cost());
        product.setImageUrl(request.imageUrl());
        product.setActive(request.active() == null || request.active());
    }

    private List<PromotionRule> liveRules() {
        return promotions.findLive().stream().map(Promotion::toRule).toList();
    }

    private ProductView view(Product p, List<PromotionRule> live, boolean showCost) {
        BigDecimal discount = PromotionEngine.discountPerUnit(p.getPrice(), p.getId(), p.getCategoryId(), live, Instant.now());
        BigDecimal rate = p.getTaxRate() == null ? BigDecimal.ZERO : p.getTaxRate();
        boolean inclusive = p.getTaxInclusive() == null || p.getTaxInclusive();
        return new ProductView(p.getId(), p.getSku(), p.getBarcode(), p.getName(), p.getDescription(),
                p.getCategoryId(), p.getTaxRateId(), rate, inclusive, p.getUom(), p.getPrice(),
                showCost ? p.getCost() : null, discount, p.getPrice().subtract(discount),
                p.getImageUrl(), p.isActive(),
                p.getTaxRateId() == null ? null : new TaxInfo(p.getTaxRateId(), p.getTaxCode(), rate, inclusive));
    }

    private boolean canSeeCost() {
        return CurrentUser.find().map(u -> u.hasAnyRole(Roles.ADMIN, Roles.MANAGER)).orElse(false);
    }

    // ------------------------------------------------------------------ categories
    @Transactional(readOnly = true)
    public List<Category> listCategories() {
        return categories.findAll();
    }

    public Category createCategory(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.name().trim());
        category.setParentId(request.parentId());
        categories.insert(category);
        return categories.findById(category.getId());
    }

    public Category updateCategory(Long id, CategoryRequest request) {
        Category category = categories.findById(id);
        if (category == null) {
            throw ApiException.notFound("Category", id);
        }
        if (id.equals(request.parentId())) {
            throw ApiException.badRequest("A category cannot be its own parent");
        }
        category.setName(request.name().trim());
        category.setParentId(request.parentId());
        categories.update(category);
        evictCatalogCache();
        return categories.findById(id);
    }

    public Category setCategoryActive(Long id, boolean active) {
        if (categories.findById(id) == null) {
            throw ApiException.notFound("Category", id);
        }
        categories.setActive(id, active);
        evictCatalogCache();
        return categories.findById(id);
    }

    // ------------------------------------------------------------------ tax rates
    @Transactional(readOnly = true)
    public List<TaxRate> listTaxRates() {
        return taxRates.findAll();
    }

    public TaxRate createTaxRate(TaxRateRequest request) {
        TaxRate taxRate = new TaxRate();
        applyTaxRate(taxRate, request);
        taxRates.insert(taxRate);
        return taxRates.findById(taxRate.getId());
    }

    public TaxRate updateTaxRate(Long id, TaxRateRequest request) {
        TaxRate taxRate = taxRates.findById(id);
        if (taxRate == null) {
            throw ApiException.notFound("Tax rate", id);
        }
        applyTaxRate(taxRate, request);
        taxRates.update(taxRate);
        evictCatalogCache();
        return taxRates.findById(id);
    }

    private void applyTaxRate(TaxRate taxRate, TaxRateRequest request) {
        taxRate.setCode(request.code().trim().toUpperCase());
        taxRate.setName(request.name().trim());
        taxRate.setRate(request.rate());
        taxRate.setInclusive(request.inclusive());
        taxRate.setActive(request.active() == null || request.active());
    }

    // ------------------------------------------------------------------ promotions
    @Transactional(readOnly = true)
    public List<Promotion> listPromotions(boolean liveOnly) {
        return liveOnly ? promotions.findLive() : promotions.findAll();
    }

    public Promotion createPromotion(PromotionRequest request) {
        Promotion promotion = new Promotion();
        applyPromotion(promotion, request);
        promotions.insert(promotion);
        evictCatalogCache();
        return promotions.findById(promotion.getId());
    }

    public Promotion updatePromotion(Long id, PromotionRequest request) {
        Promotion promotion = promotions.findById(id);
        if (promotion == null) {
            throw ApiException.notFound("Promotion", id);
        }
        applyPromotion(promotion, request);
        promotions.update(promotion);
        evictCatalogCache();
        return promotions.findById(id);
    }

    private void applyPromotion(Promotion promotion, PromotionRequest request) {
        if (request.productId() != null && request.categoryId() != null) {
            throw ApiException.badRequest("A promotion targets either a product or a category, not both");
        }
        switch (request.type()) {
            case PERCENT -> {
                requireValue(request);
                if (request.value().compareTo(new BigDecimal("100")) > 0) {
                    throw ApiException.badRequest("A percentage promotion cannot exceed 100");
                }
            }
            case FIXED -> requireValue(request);
            case BUY_X_GET_Y -> {
                if (request.buyQty() == null || request.getQty() == null) {
                    throw ApiException.badRequest("A buy-X-get-Y promotion needs buyQty and getQty");
                }
                if (request.productId() == null && request.categoryId() == null) {
                    throw ApiException.badRequest("A buy-X-get-Y promotion must target a product or a category");
                }
            }
        }
        if (request.startsAt() != null && request.endsAt() != null && !request.endsAt().isAfter(request.startsAt())) {
            throw ApiException.badRequest("endsAt must be after startsAt");
        }
        promotion.setCode(request.code().trim().toUpperCase());
        promotion.setName(request.name().trim());
        promotion.setType(request.type());
        boolean bogo = request.type() == com.pos.catalog.domain.PromotionType.BUY_X_GET_Y;
        promotion.setValue(bogo ? null : request.value());
        promotion.setBuyQty(bogo ? request.buyQty() : null);
        promotion.setGetQty(bogo ? request.getQty() : null);
        promotion.setMinQty(request.minQty());
        promotion.setStoreId(request.storeId());
        promotion.setCouponCode(request.couponCode() == null || request.couponCode().isBlank()
                ? null : request.couponCode().trim().toUpperCase());
        promotion.setProductId(request.productId());
        promotion.setCategoryId(request.categoryId());
        promotion.setStartsAt(request.startsAt());
        promotion.setEndsAt(request.endsAt());
        promotion.setActive(request.active() == null || request.active());
    }

    private static void requireValue(PromotionRequest request) {
        if (request.value() == null) {
            throw ApiException.badRequest("A " + request.type() + " promotion needs a value");
        }
    }
}
