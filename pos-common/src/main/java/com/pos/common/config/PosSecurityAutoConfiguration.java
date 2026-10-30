package com.pos.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.pos.common.security.JwtProperties;
import com.pos.common.security.JwtService;

/** Registers the {@link JwtService} in every service (servlet or reactive). */
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
public class PosSecurityAutoConfiguration {

    @Bean
    public JwtService jwtService(JwtProperties properties) {
        return new JwtService(properties);
    }
}
