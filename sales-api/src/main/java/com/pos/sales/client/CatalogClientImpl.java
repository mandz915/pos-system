package com.pos.sales.client;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.pos.common.cache.CacheNames;
import com.pos.common.tenant.TenantContext;
import com.pos.common.cache.JsonCache;
import com.pos.common.error.ApiException;
import com.pos.common.pricing.PromotionRule;
import com.pos.common.security.CurrentUser;

public class CatalogClientImpl implements CatalogClient {

    private static final Duration PROMOTIONS_LOCAL_TTL = Duration.ofSeconds(10);
    private static final Duration PROMOTIONS_SHARED_TTL = Duration.ofSeconds(30);

    private final RestClient rest;
    private final JsonCache cache;

    private final ConcurrentHashMap<Long, PromotionSnapshot> promotions = new ConcurrentHashMap<>();

    public CatalogClientImpl(RestClient rest, JsonCache cache) {
        this.rest = rest;
        this.cache = cache;
    }

    private record PromotionSnapshot(List<PromotionRule> rules, long loadedAtMillis) {
    }

    @Override
    public List<PromotionRule> livePromotions() {
        Long tenant = TenantContext.require();
        PromotionSnapshot local = promotions.get(tenant);
        long now = System.currentTimeMillis();
        if (local != null && now - local.loadedAtMillis() < PROMOTIONS_LOCAL_TTL.toMillis()) {
            return local.rules();
        }
        List<PromotionRule> rules = cache.get(CacheNames.CATALOG, "promotions:live", PromotionRule[].class)
                .map(List::of)
                .orElse(null);
        if (rules == null) {
            rules = fetchLivePromotions();
            cache.put(CacheNames.CATALOG, "promotions:live", rules.toArray(new PromotionRule[0]), PROMOTIONS_SHARED_TTL);
        }
        promotions.put(tenant, new PromotionSnapshot(rules, now));
        return rules;
    }

    private List<PromotionRule> fetchLivePromotions() {
        try {
            PromotionRule[] body = rest.get()
                    .uri("/api/catalog/promotions?liveOnly=true")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + CurrentUser.token())
                    .retrieve()
                    .body(PromotionRule[].class);
            return body == null ? List.of() : List.of(body);
        } catch (HttpStatusCodeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Catalog service error (" + e.getStatusCode().value() + ")");
        } catch (ResourceAccessException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Catalog service is unavailable");
        }
    }

    @Override
    public ProductInfo getProduct(Long productId) {
        ProductInfo cached = cache.get(CacheNames.CATALOG, CacheNames.productById(productId), ProductInfo.class).orElse(null);
        if (cached != null) {
            return cached;
        }
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
