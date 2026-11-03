package com.pos.catalog.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pos.catalog.dto.Category;
import com.pos.catalog.dto.Promotion;
import com.pos.catalog.dto.TaxRate;
import com.pos.catalog.service.CatalogService;
import com.pos.catalog.dto.CatalogDtos.CategoryRequest;
import com.pos.catalog.dto.CatalogDtos.ProductRequest;
import com.pos.catalog.dto.CatalogDtos.ProductView;
import com.pos.catalog.dto.CatalogDtos.PromotionRequest;
import com.pos.catalog.dto.CatalogDtos.TaxRateRequest;
import com.pos.common.security.RequireRole;
import com.pos.common.security.Roles;
import com.pos.common.dto.ActiveRequest;
import com.pos.common.dto.PageResult;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalog;

    @GetMapping("/products")
    public PageResult<ProductView> products(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) Long categoryId,
                                            @RequestParam(required = false) Boolean active,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        return catalog.searchProducts(q, categoryId, active, page, size);
    }

    @GetMapping("/products/{id}")
    public ProductView product(@PathVariable Long id) {
        return catalog.getProduct(id);
    }

    @GetMapping("/products/barcode/{barcode}")
    public ProductView productByBarcode(@PathVariable String barcode) {
        return catalog.getByBarcode(barcode);
    }

    @GetMapping("/products/sku/{sku}")
    public ProductView productBySku(@PathVariable String sku) {
        return catalog.getBySku(sku);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public ProductView createProduct(@Valid @RequestBody ProductRequest request) {
        return catalog.createProduct(request);
    }

    @PutMapping("/products/{id}")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public ProductView updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return catalog.updateProduct(id, request);
    }

    @PatchMapping("/products/{id}/active")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public ProductView setProductActive(@PathVariable Long id, @RequestBody ActiveRequest request) {
        return catalog.setProductActive(id, request.active());
    }

    @GetMapping("/categories")
    public List<Category> categories() {
        return catalog.listCategories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public Category createCategory(@Valid @RequestBody CategoryRequest request) {
        return catalog.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public Category updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return catalog.updateCategory(id, request);
    }

    @PatchMapping("/categories/{id}/active")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public Category setCategoryActive(@PathVariable Long id, @RequestBody ActiveRequest request) {
        return catalog.setCategoryActive(id, request.active());
    }

    @GetMapping("/tax-rates")
    public List<TaxRate> taxRates() {
        return catalog.listTaxRates();
    }

    @PostMapping("/tax-rates")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole(Roles.ADMIN)
    public TaxRate createTaxRate(@Valid @RequestBody TaxRateRequest request) {
        return catalog.createTaxRate(request);
    }

    @PutMapping("/tax-rates/{id}")
    @RequireRole(Roles.ADMIN)
    public TaxRate updateTaxRate(@PathVariable Long id, @Valid @RequestBody TaxRateRequest request) {
        return catalog.updateTaxRate(id, request);
    }

    @GetMapping("/promotions")
    public List<Promotion> promotions(@RequestParam(defaultValue = "false") boolean liveOnly) {
        return catalog.listPromotions(liveOnly);
    }

    @PostMapping("/promotions")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public Promotion createPromotion(@Valid @RequestBody PromotionRequest request) {
        return catalog.createPromotion(request);
    }

    @PutMapping("/promotions/{id}")
    @RequireRole({Roles.ADMIN, Roles.MANAGER})
    public Promotion updatePromotion(@PathVariable Long id, @Valid @RequestBody PromotionRequest request) {
        return catalog.updatePromotion(id, request);
    }
}
