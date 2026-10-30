package com.pos.sales.client;

import java.math.BigDecimal;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pos.common.error.ApiException;
import com.pos.common.security.CurrentUser;

/** Reads authoritative prices and tax settings from catalog-api, on behalf of the signed-in user. */
public class CatalogClient {

    private final RestClient rest;

    public CatalogClient(RestClient rest) {
        this.rest = rest;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductInfo(
            Long id,
            String sku,
            String name,
            BigDecimal price,
            BigDecimal taxRate,
            boolean taxInclusive,
            BigDecimal discountPerUnit,
            boolean active) {
    }

    public ProductInfo getProduct(Long productId) {
        try {
            ProductInfo product = rest.get()
                    .uri("/api/catalog/products/{id}", productId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + CurrentUser.token())
                    .retrieve()
                    .body(ProductInfo.class);
            if (product == null) {
                throw ApiException.badRequest("Unknown product " + productId);
            }
            return product;
        } catch (HttpClientErrorException.NotFound e) {
            throw ApiException.badRequest("Unknown product " + productId);
        } catch (HttpStatusCodeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Catalog service error (" + e.getStatusCode().value() + ")");
        } catch (ResourceAccessException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Catalog service is unavailable");
        }
    }
}
