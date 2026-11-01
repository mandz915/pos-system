package com.pos.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.pos.catalog.domain.Promotion;
import com.pos.catalog.domain.PromotionType;
import com.pos.catalog.service.CatalogService;
import com.pos.catalog.dto.CatalogDtos.ProductView;
import com.pos.catalog.dto.CatalogDtos.PromotionRequest;
import com.pos.catalog.dto.CatalogDtos.TaxInfo;
import com.pos.common.error.ApiException;
import com.pos.test.ApiTestSupport;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogControllerTest {

    @Mock
    private CatalogService catalog;

    private MockMvc mvc;

    private static ProductView coffee(TaxInfo tax) {
        return new ProductView(5L, "COFFEE-001", "4800000000011", "Brewed Coffee", null, 1L,
                tax == null ? null : tax.id(), tax == null ? BigDecimal.ZERO : tax.rate(), true, "pc",
                new BigDecimal("120.00"), null, BigDecimal.ZERO, new BigDecimal("120.00"), null, true, tax);
    }

    @BeforeEach
    void setUp() {
        mvc = ApiTestSupport.mockMvc(new CatalogController(catalog));
    }

    @Test
    void aCashierCanLookUpAProductAndSeesItsTaxObject() throws Exception {
        when(catalog.getProduct(5L)).thenReturn(coffee(new TaxInfo(1L, "VAT12", new BigDecimal("12.00"), true)));

        mvc.perform(get("/api/catalog/products/5").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.cashier()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("COFFEE-001"))
                .andExpect(jsonPath("$.tax.code").value("VAT12"))
                .andExpect(jsonPath("$.tax.inclusive").value(true))
                .andExpect(jsonPath("$.cost").doesNotExist());
    }

    @Test
    void theTaxObjectIsOptionalSoAnUntaxedProductHasNone() throws Exception {
        when(catalog.getProduct(5L)).thenReturn(coffee(null));

        mvc.perform(get("/api/catalog/products/5").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.cashier()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tax").doesNotExist());
    }

    @Test
    void anUnknownBarcodeIs404() throws Exception {
        when(catalog.getByBarcode("000")).thenThrow(ApiException.notFound("Product with barcode", "000"));

        mvc.perform(get("/api/catalog/products/barcode/000").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.cashier()))
                .andExpect(status().isNotFound());
    }

    @Test
    void aCashierCannotCreateProducts() throws Exception {
        mvc.perform(post("/api/catalog/products").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.cashier())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"X\",\"name\":\"X\",\"price\":1}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(catalog);
    }

    @Test
    void aManagerCreatesAProduct() throws Exception {
        when(catalog.createProduct(any())).thenReturn(coffee(null));

        mvc.perform(post("/api/catalog/products").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.manager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"COFFEE-001\",\"name\":\"Brewed Coffee\",\"price\":120.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void aProductWithoutANameIsRejected() throws Exception {
        mvc.perform(post("/api/catalog/products").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.manager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"COFFEE-001\",\"price\":120.00}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(catalog);
    }

    @Test
    void aBuyXGetYCouponPromotionReachesTheServiceIntact() throws Exception {
        when(catalog.createPromotion(any())).thenReturn(new Promotion());

        mvc.perform(post("/api/catalog/promotions").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.manager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"B2G1","name":"Buy 2 get 1","type":"BUY_X_GET_Y",
                                 "buyQty":2,"getQty":1,"minQty":3,"productId":5,"storeId":1,"couponCode":"welcome"}
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<PromotionRequest> captor = ArgumentCaptor.forClass(PromotionRequest.class);
        verify(catalog).createPromotion(captor.capture());
        PromotionRequest request = captor.getValue();
        assertThat(request.type()).isEqualTo(PromotionType.BUY_X_GET_Y);
        assertThat(request.buyQty()).isEqualTo(2);
        assertThat(request.getQty()).isEqualTo(1);
        assertThat(request.minQty()).isEqualTo(3);
        assertThat(request.storeId()).isEqualTo(1L);
        assertThat(request.couponCode()).isEqualTo("welcome");
        assertThat(request.value()).isNull();
    }

    @Test
    void aCashierCannotCreatePromotions() throws Exception {
        mvc.perform(post("/api/catalog/promotions").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.cashier())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"X\",\"name\":\"X\",\"type\":\"PERCENT\",\"value\":10}"))
                .andExpect(status().isForbidden());
    }
}
