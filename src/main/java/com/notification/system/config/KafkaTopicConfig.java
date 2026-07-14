package com.notification.system.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the topic explicitly (auto-topic-creation is disabled on the
 * broker — see docker-compose.yml) so partition count/replication are a
 * reviewable code decision, not whatever the first producer happened to get
 * by default.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${notification.kafka.topic}")
    private String notificationTopic;

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name(notificationTopic)
                .partitions(3)
                .replicas(1) // single local broker; would be >=3 in a real cluster
                .build();
    }
}
