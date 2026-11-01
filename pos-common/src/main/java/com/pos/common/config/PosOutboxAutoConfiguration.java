package com.pos.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import com.pos.common.outbox.OutboxRelay;
import com.pos.common.outbox.OutboxService;

/** Enabled with {@code pos.outbox.enabled=true}; requires an {@code outbox} table (see Flyway V1 of the service). */
@AutoConfiguration
@ConditionalOnProperty(prefix = "pos.outbox", name = "enabled", havingValue = "true")
@EnableScheduling
public class PosOutboxAutoConfiguration {

    @Bean
    public OutboxService outboxService(JdbcTemplate jdbcTemplate) {
        return new OutboxService(jdbcTemplate);
    }

    @Bean
    public OutboxRelay outboxRelay(JdbcTemplate jdbcTemplate,
                                   KafkaTemplate<String, String> kafkaTemplate,
                                   PlatformTransactionManager transactionManager,
                                   @Value("${pos.outbox.batch-size:500}") int batchSize) {
        return new OutboxRelay(jdbcTemplate, kafkaTemplate, transactionManager, batchSize);
    }
}
