package com.notification.system.consumer;

import com.notification.system.queue.NotificationQueueMessage;
import com.notification.system.service.NotificationProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationConsumer {

    private final NotificationProcessor notificationProcessor;

    public NotificationConsumer(NotificationProcessor notificationProcessor) {
        this.notificationProcessor = notificationProcessor;
    }

    @KafkaListener(
            topics = "${notification.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(NotificationQueueMessage message, Acknowledgment acknowledgment) {
        log.debug("Received notification id={} trigger={}", message.notificationId(), message.triggeredBy());
        notificationProcessor.process(message);
        // Manual ack (ack-mode: manual_immediate) so a processing exception leaves the
        // offset uncommitted and the message gets redelivered instead of silently lost.
        acknowledgment.acknowledge();
    }
}
