package com.pos.common.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.listener.RecordInterceptor;

import com.pos.common.tenant.TenantAwareDataSource;
import com.pos.common.tenant.TenantIsolationGuard;
import com.pos.common.tenant.TenantRecordInterceptor;

@AutoConfiguration(afterName = {
        "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration",
        "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration"})
public class PosTenancyAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({DataSource.class, TenantAwareDataSource.class})
    @ConditionalOnProperty(prefix = "pos.tenancy", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class Database {

        @Bean
        static BeanPostProcessor posTenantDataSourcePostProcessor() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean instanceof DataSource dataSource && !(bean instanceof TenantAwareDataSource)) {
                        return new TenantAwareDataSource(dataSource);
                    }
                    return bean;
                }
            };
        }

        @Bean
        @ConditionalOnBean(JdbcTemplate.class)
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "pos.tenancy", name = "enforce-rls", havingValue = "true", matchIfMissing = true)
        TenantIsolationGuard posTenantIsolationGuard(JdbcTemplate jdbc) {
            return new TenantIsolationGuard(jdbc);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RecordInterceptor.class)
    @ConditionalOnProperty(prefix = "pos.tenancy", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class Kafka {

        @Bean
        @ConditionalOnMissingBean(RecordInterceptor.class)
        RecordInterceptor<Object, Object> posTenantRecordInterceptor() {
            return new TenantRecordInterceptor();
        }
    }
}
