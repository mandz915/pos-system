package com.pos.sales.client;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.pos.common.cache.JsonCache;

@Configuration
public class ClientConfig {

    @Bean
    public CatalogClient catalogClient(@Value("${pos.services.catalog-url}") String baseUrl,
                                       ObjectProvider<JsonCache> cache) {
        return new CatalogClientImpl(restClient(baseUrl), cache.getIfAvailable(() -> JsonCache.NONE));
    }

    @Bean
    public PaymentClient paymentClient(@Value("${pos.services.payment-url}") String baseUrl) {
        return new PaymentClientImpl(restClient(baseUrl));
    }

    private static RestClient restClient(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
