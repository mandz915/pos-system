package com.pos.sales.client;

import java.math.BigDecimal;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pos.common.error.ApiException;
import com.pos.common.model.PaymentMethod;
import com.pos.common.security.CurrentUser;
import com.pos.sales.service.TenderPlanner.Tender;

/** Talks to payment-api on behalf of the signed-in user. */
public class PaymentClient {

    private final RestClient rest;

    public PaymentClient(RestClient rest) {
        this.rest = rest;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentResult(Long id, String status, String gatewayRef, String reason) {
    }

    public record ChargeBody(Long saleId, Long storeId, PaymentMethod method, BigDecimal amount, BigDecimal tendered,
                             String reference) {
    }

    public record RefundBody(Long saleId, Long storeId, PaymentMethod method, BigDecimal amount, String reason) {
    }

    public PaymentResult charge(Long saleId, Long storeId, Tender tender) {
        return call(rest.post()
                .uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChargeBody(saleId, storeId, tender.method(), tender.applied(), tender.tendered(),
                        tender.reference())));
    }

    /** Money back to the customer for returned goods (supervisor level in payment-api). */
    public PaymentResult refund(Long saleId, Long storeId, PaymentMethod method, BigDecimal amount, String reason) {
        return call(rest.post()
                .uri("/api/payments/refunds")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefundBody(saleId, storeId, method, amount, reason)));
    }

    /** Cancels an approved payment of the current user, used to roll back a checkout that could not complete. */
    public PaymentResult reverse(Long paymentId) {
        return call(rest.post().uri("/api/payments/{id}/reverse", paymentId));
    }

    private PaymentResult call(RestClient.RequestBodySpec spec) {
        try {
            return spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + CurrentUser.token())
                    .retrieve()
                    .body(PaymentResult.class);
        } catch (HttpStatusCodeException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            if (status == null || !status.is4xxClientError()) {
                status = HttpStatus.BAD_GATEWAY;
            }
            throw new ApiException(status, "Payment service: " + summarize(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Payment service is unavailable");
        }
    }

    private static String summarize(String body) {
        if (body == null || body.isBlank()) {
            return "request failed";
        }
        return body.length() > 200 ? body.substring(0, 200) : body;
    }
}
