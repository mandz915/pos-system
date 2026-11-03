package com.pos.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.pos.common.cache.JsonCache;
import com.pos.common.cache.RedisCircuit;
import com.pos.common.cache.RedisJsonCache;
import com.pos.common.cache.RedisTokenRevocations;
import com.pos.common.security.JwtService;
import com.pos.common.security.TokenRevocations;

@AutoConfiguration(afterName = {
        "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"})
public class PosCacheAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "pos.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class RedisBeans {

        @Bean
        @ConditionalOnBean(StringRedisTemplate.class)
        RedisCircuit posRedisCircuit() {
            return new RedisCircuit(5, 5_000);
        }

        @Bean
        @ConditionalOnBean(StringRedisTemplate.class)
        JsonCache posJsonCache(StringRedisTemplate redis, RedisCircuit circuit) {
            return new RedisJsonCache(redis, circuit);
        }

        @Bean
        @ConditionalOnBean(StringRedisTemplate.class)
        TokenRevocations posTokenRevocations(StringRedisTemplate redis, RedisCircuit circuit, JwtService jwt) {
            return new RedisTokenRevocations(redis, circuit, jwt.getTtl());
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonCache posNoJsonCache() {
        return JsonCache.NONE;
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenRevocations posNoTokenRevocations() {
        return TokenRevocations.NONE;
    }
}
