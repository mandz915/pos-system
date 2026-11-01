package com.pos.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import com.pos.common.event.Topics;

/**
 * Creates the event topics with several partitions so consumers can run in parallel (a topic created implicitly by
 * the broker has one partition and would cap throughput at one consumer thread). Idempotent: every service may
 * declare them. Partitions can only be increased later, never decreased.
 */
@AutoConfiguration
@ConditionalOnClass(KafkaAdmin.class)
@ConditionalOnProperty(prefix = "pos.kafka", name = "create-topics", havingValue = "true", matchIfMissing = true)
public class PosKafkaTopicsAutoConfiguration {

    @Bean
    NewTopic saleCompletedTopic(@Value("${pos.kafka.partitions:6}") int partitions,
                                @Value("${pos.kafka.replicas:1}") int replicas) {
        return topic(Topics.SALE_COMPLETED, partitions, replicas);
    }

    @Bean
    NewTopic saleRefundedTopic(@Value("${pos.kafka.partitions:6}") int partitions,
                               @Value("${pos.kafka.replicas:1}") int replicas) {
        return topic(Topics.SALE_REFUNDED, partitions, replicas);
    }

    @Bean
    NewTopic goodsReceivedTopic(@Value("${pos.kafka.partitions:6}") int partitions,
                                @Value("${pos.kafka.replicas:1}") int replicas) {
        return topic(Topics.GOODS_RECEIVED, partitions, replicas);
    }

    @Bean
    NewTopic lowStockTopic(@Value("${pos.kafka.partitions:6}") int partitions,
                           @Value("${pos.kafka.replicas:1}") int replicas) {
        return topic(Topics.LOW_STOCK, partitions, replicas);
    }

    private static NewTopic topic(String name, int partitions, int replicas) {
        return TopicBuilder.name(name).partitions(partitions).replicas(replicas).build();
    }
}
