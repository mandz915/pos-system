package com.pos.common.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.pos.common.security.JwtService;
import com.pos.common.security.TokenRevocations;
import com.pos.common.controller.AuthInterceptor;
import com.pos.common.controller.GlobalExceptionHandler;

@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
public class PosWebMvcAutoConfiguration {

    @Bean
    public AuthInterceptor posAuthInterceptor(JwtService jwtService, ObjectProvider<TokenRevocations> revocations) {
        return new AuthInterceptor(jwtService, revocations.getIfAvailable(() -> TokenRevocations.NONE));
    }

    @Bean
    public GlobalExceptionHandler posGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public WebMvcConfigurer posWebMvcConfigurer(AuthInterceptor interceptor, Environment environment) {
        List<String> publicPaths = Arrays.stream(environment.getProperty("pos.security.public-paths", "").split(","))
                .map(String::trim)
                .filter(path -> !path.isEmpty())
                .toList();
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor)
                        .addPathPatterns("/api/**")
                        .excludePathPatterns(publicPaths);
            }
        };
    }
}
