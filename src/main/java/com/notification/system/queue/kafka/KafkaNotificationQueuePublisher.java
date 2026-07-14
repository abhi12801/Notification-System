package com.notification.system.queue.kafka;

import com.notification.system.queue.NotificationQueueMessage;
import com.notification.system.queue.NotificationQueuePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaNotificationQueuePublisher implements NotificationQueuePublisher {

    private final KafkaTemplate<String, NotificationQueueMessage> kafkaTemplate;
    private final String topic;

    public KafkaNotificationQueuePublisher(
            KafkaTemplate<String, NotificationQueueMessage> kafkaTemplate,
            @Value("${notification.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(NotificationQueueMessage message) {
        // Keyed by notificationId: Kafka routes same-key messages to the same
        // partition, so retries for one notification are never processed
        // out of order relative to its initial send.
        String key = String.valueOf(message.notificationId());

        kafkaTemplate.send(topic, key, message).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish notification id={} to topic={}", message.notificationId(), topic, ex);
            } else {
                log.debug("Published notification id={} to topic={} partition={} offset={}",
                        message.notificationId(), topic,
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
