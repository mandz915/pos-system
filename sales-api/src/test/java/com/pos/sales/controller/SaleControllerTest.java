package com.pos.sales.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

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

import com.pos.common.error.ApiException;
import com.pos.sales.dto.Sale;
import com.pos.sales.dto.SaleStatus;
import com.pos.sales.service.ReceiptService;
import com.pos.sales.service.SaleService;
import com.pos.sales.dto.SalesDtos.CheckoutRequest;
import com.pos.sales.dto.SalesDtos.CreateSaleRequest;
import com.pos.sales.dto.SalesDtos.SaleView;
import com.pos.test.ApiTestSupport;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SaleControllerTest {

    @Mock
    private SaleService saleService;
    @Mock
    private ReceiptService receiptService;

    private MockMvc mvc;

    private static SaleView saleView(long id) {
        Sale sale = new Sale();
        sale.setId(id);
        sale.setStoreId(1L);
        sale.setStatus(SaleStatus.OPEN);
        sale.setTotal(new BigDecimal("180.00"));
        return new SaleView(sale, List.of(), List.of(), List.of());
    }

    @BeforeEach
    void setUp() {
        mvc = ApiTestSupport.mockMvc(new SaleController(saleService, receiptService));
    }

    @Test
    void theTillCreatesASaleWithCouponsAndAnOptionalLineTax() throws Exception {
        when(saleService.create(any())).thenReturn(saleView(77));

        mvc.perform(post("/api/sales").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.supervisor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientRef":"7f9c-1","storeId":1,"terminalId":1,
                                 "couponCodes":["WELCOME20"],
                                 "lines":[{"productId":5,"quantity":2},
                                          {"productId":6,"quantity":1,"tax":{"code":"EXEMPT","rate":0,"inclusive":true}}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sale.id").value(77));

        ArgumentCaptor<CreateSaleRequest> captor = ArgumentCaptor.forClass(CreateSaleRequest.class);
        verify(saleService).create(captor.capture());
        CreateSaleRequest request = captor.getValue();
        assertThat(request.couponCodes()).containsExactly("WELCOME20");
        assertThat(request.lines()).hasSize(2);
        assertThat(request.lines().get(0).tax()).isNull();
        assertThat(request.lines().get(1).tax().code()).isEqualTo("EXEMPT");
        assertThat(request.lines().get(1).tax().rate()).isEqualByComparingTo("0");
    }

    @Test
    void couponsAndLineTaxAreBothOptional() throws Exception {
        when(saleService.create(any())).thenReturn(saleView(78));

        mvc.perform(post("/api/sales").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.cashier())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRef\":\"a\",\"storeId\":1,\"terminalId\":1,\"lines\":[{\"productId\":5,\"quantity\":1}]}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateSaleRequest> captor = ArgumentCaptor.forClass(CreateSaleRequest.class);
        verify(saleService).create(captor.capture());
        assertThat(captor.getValue().couponCodes()).isNull();
    }

    @Test
    void aSaleWithoutLinesIsRejected() throws Exception {
        mvc.perform(post("/api/sales").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.cashier())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRef\":\"a\",\"storeId\":1,\"terminalId\":1,\"lines\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(saleService);
    }

    @Test
    void aTaxRateAbove100PercentIsRejected() throws Exception {
        mvc.perform(post("/api/sales").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.supervisor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRef\":\"a\",\"storeId\":1,\"terminalId\":1,"
                                + "\"lines\":[{\"productId\":5,\"quantity\":1,\"tax\":{\"rate\":150}}]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(saleService);
    }

    @Test
    void salesNeedAToken() throws Exception {
        mvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkoutPassesTheSplitPaymentsToTheService() throws Exception {
        when(saleService.checkout(eq(77L), any())).thenReturn(saleView(77));

        mvc.perform(post("/api/sales/77/checkout").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.cashier())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payments\":[{\"method\":\"EWALLET\",\"amount\":30.00,\"reference\":\"gc-1\"},"
                                + "{\"method\":\"CASH\",\"amount\":200.00}]}"))
                .andExpect(status().isOk());

        ArgumentCaptor<CheckoutRequest> captor = ArgumentCaptor.forClass(CheckoutRequest.class);
        verify(saleService).checkout(eq(77L), captor.capture());
        assertThat(captor.getValue().payments()).hasSize(2);
    }

    @Test
    void aDeclinedPaymentIsReturnedAs402() throws Exception {
        when(saleService.checkout(eq(77L), any()))
                .thenThrow(new ApiException(org.springframework.http.HttpStatus.PAYMENT_REQUIRED, "CARD payment was declined"));

        mvc.perform(post("/api/sales/77/checkout").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.cashier())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payments\":[{\"method\":\"CARD\",\"amount\":180.00}]}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.message").value("CARD payment was declined"));
    }

    @Test
    void aCashierCannotRefund() throws Exception {
        mvc.perform(post("/api/sales/77/refund").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.cashier())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"changed mind\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(saleService);
    }

    @Test
    void aSupervisorCanRefund() throws Exception {
        when(saleService.refund(eq(77L), any())).thenReturn(saleView(77));

        mvc.perform(post("/api/sales/77/refund").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.supervisor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"changed mind\",\"method\":\"CASH\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void theReceiptIsPlainText() throws Exception {
        when(receiptService.render(77L)).thenReturn("POS System\nTOTAL 180.00");

        mvc.perform(get("/api/sales/77/receipt").header(HttpHeaders.AUTHORIZATION, ApiTestSupport.cashier()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("POS System\nTOTAL 180.00"));
    }
}
