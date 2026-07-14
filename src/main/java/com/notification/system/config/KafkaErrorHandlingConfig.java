package com.notification.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Handles infrastructure-level failures (a DB hiccup during processing,
 * deserialization errors) — NOT the simulated 30% "business" failure, which
 * is a normal, successfully-processed outcome recorded as status=FAILED.
 * Retries 3 times, 1s apart, then logs and moves on (no DLQ topic — out of
 * scope per the spec; see README "Known limitations").
 */
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(1000L, 3));
    }
}
