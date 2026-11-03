package com.pos.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.pos.catalog.dto.Product;
import com.pos.catalog.dto.Promotion;
import com.pos.catalog.dto.PromotionType;
import com.pos.catalog.mapper.CategoryMapper;
import com.pos.catalog.mapper.ProductMapper;
import com.pos.catalog.mapper.PromotionMapper;
import com.pos.catalog.mapper.TaxRateMapper;
import com.pos.catalog.dto.CatalogDtos.ProductRequest;
import com.pos.catalog.dto.CatalogDtos.ProductView;
import com.pos.catalog.dto.CatalogDtos.PromotionRequest;
import com.pos.common.cache.CacheNames;
import com.pos.common.cache.JsonCache;
import com.pos.common.error.ApiException;
import com.pos.common.security.AuthenticatedUser;
import com.pos.common.security.CurrentUser;
import com.pos.common.security.Roles;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogServiceTest {

    @Mock
    private ProductMapper products;
    @Mock
    private CategoryMapper categories;
    @Mock
    private TaxRateMapper taxRates;
    @Mock
    private PromotionMapper promotions;
    @Mock
    private JsonCache cache;

    private CatalogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CatalogServiceImpl(products, categories, taxRates, promotions, cache);
        asCashier();
        Product coffee = new Product();
        coffee.setId(5L);
        coffee.setSku("COFFEE");
        coffee.setName("Brewed Coffee");
        coffee.setPrice(new BigDecimal("120.00"));
        coffee.setCost(new BigDecimal("35.00"));
        coffee.setTaxRateId(1L);
        coffee.setTaxCode("VAT12");
        coffee.setTaxRate(new BigDecimal("12.00"));
        coffee.setTaxInclusive(true);
        when(products.findById(5L)).thenReturn(coffee);
        when(cache.get(anyString(), anyString(), eq(ProductView.class))).thenReturn(Optional.empty());
    }

    @AfterEach
    void clear() {
        CurrentUser.clear();
    }

    private static void asCashier() {
        CurrentUser.set(new AuthenticatedUser(10L, "cora", Set.of(Roles.CASHIER), 1L, 1L), "token");
    }

    private static void asManager() {
        CurrentUser.set(new AuthenticatedUser(12L, "mia", Set.of(Roles.MANAGER), 1L, 1L), "token");
    }

    @Test
    void aCacheHitAnswersWithoutTouchingTheDatabase() {
        ProductView cached = new ProductView(5L, "COFFEE", null, "Brewed Coffee", null, null, 1L, new BigDecimal("12.00"),
                true, "pc", new BigDecimal("120.00"), null, BigDecimal.ZERO, new BigDecimal("120.00"), null, true, null);
        when(cache.get(CacheNames.CATALOG, "product:5", ProductView.class)).thenReturn(Optional.of(cached));

        assertThat(service.getProduct(5L)).isSameAs(cached);

        verifyNoInteractions(products, promotions);
    }

    @Test
    void aMissLoadsFromTheDatabaseAndCachesTheViewWithoutCostAndWithTheTaxObject() {
        ProductView view = service.getProduct(5L);

        assertThat(view.cost()).isNull();
        assertThat(view.tax()).isNotNull();
        assertThat(view.tax().code()).isEqualTo("VAT12");
        assertThat(view.tax().inclusive()).isTrue();
        ArgumentCaptor<Object> stored = ArgumentCaptor.forClass(Object.class);
        verify(cache).put(eq(CacheNames.CATALOG), eq("product:5"), stored.capture(), any(Duration.class));
        assertThat(((ProductView) stored.getValue()).cost()).isNull();
    }

    @Test
    void aManagerSeesTheCostButNeverCachesIt() {
        asManager();
        ProductView cached = new ProductView(5L, "COFFEE", null, "Brewed Coffee", null, null, null, BigDecimal.ZERO, true,
                "pc", new BigDecimal("120.00"), null, BigDecimal.ZERO, new BigDecimal("120.00"), null, true, null);
        when(cache.get(CacheNames.CATALOG, "product:5", ProductView.class)).thenReturn(Optional.of(cached));

        ProductView view = service.getProduct(5L);

        assertThat(view.cost()).isEqualByComparingTo("35.00");
        verify(cache, never()).get(any(), any(), any());
        ArgumentCaptor<Object> stored = ArgumentCaptor.forClass(Object.class);
        verify(cache).put(eq(CacheNames.CATALOG), eq("product:5"), stored.capture(), any(Duration.class));
        assertThat(((ProductView) stored.getValue()).cost()).isNull();
    }

    @Test
    void anUnknownProductIsA404AndNothingIsCached() {
        when(products.findById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.getProduct(9L)).isInstanceOf(ApiException.class);

        verify(cache, never()).put(any(), any(), any(), any());
    }

    @Test
    void changingAProductEvictsTheCatalogCacheAndAnswersFromTheDatabase() {
        asManager();
        ProductRequest request = new ProductRequest("COFFEE", null, "Brewed Coffee", null, null, null, null,
                new BigDecimal("125.00"), null, null, null);

        service.updateProduct(5L, request);

        verify(products).update(any(Product.class));
        verify(cache).evictAll(CacheNames.CATALOG);
        verify(cache, never()).get(any(), any(), any());
    }

    @Test
    void changingAPromotionEvictsTheCatalogCache() {
        asManager();
        PromotionRequest request = new PromotionRequest("TEN", "Ten off", PromotionType.PERCENT, new BigDecimal("10"),
                null, null, null, null, null, null, null, null, null, null);

        service.createPromotion(request);

        verify(promotions).insert(any(Promotion.class));
        verify(cache).evictAll(CacheNames.CATALOG);
    }

    @Test
    void aBuyXGetYPromotionNeedsBothQuantitiesAndATarget() {
        asManager();
        PromotionRequest noQuantities = new PromotionRequest("B", "b", PromotionType.BUY_X_GET_Y, null, null, null, null,
                5L, null, null, null, null, null, null);
        PromotionRequest noTarget = new PromotionRequest("B", "b", PromotionType.BUY_X_GET_Y, null, 2, 1, null,
                null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.createPromotion(noQuantities)).hasMessageContaining("buyQty and getQty");
        assertThatThrownBy(() -> service.createPromotion(noTarget)).hasMessageContaining("product or a category");

        verify(promotions, never()).insert(any(Promotion.class));
    }

    @Test
    void aPercentPromotionNeedsAValueAndCannotExceed100() {
        asManager();
        PromotionRequest noValue = new PromotionRequest("P", "p", PromotionType.PERCENT, null, null, null, null,
                null, null, null, null, null, null, null);
        PromotionRequest tooBig = new PromotionRequest("P", "p", PromotionType.PERCENT, new BigDecimal("101"), null, null,
                null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.createPromotion(noValue)).hasMessageContaining("needs a value");
        assertThatThrownBy(() -> service.createPromotion(tooBig)).hasMessageContaining("cannot exceed 100");
    }

    @Test
    void couponCodesAreStoredUpperCaseAndTrimmed() {
        asManager();
        PromotionRequest request = new PromotionRequest("W", "Welcome", PromotionType.FIXED, new BigDecimal("15"), null,
                null, null, null, null, 1L, "  welcome20 ", null, null, null);

        service.createPromotion(request);

        ArgumentCaptor<Promotion> captor = ArgumentCaptor.forClass(Promotion.class);
        verify(promotions).insert(captor.capture());
        assertThat(captor.getValue().getCouponCode()).isEqualTo("WELCOME20");
        assertThat(captor.getValue().getStoreId()).isEqualTo(1L);
    }
}
