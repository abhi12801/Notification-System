package com.notification.system.queue;

/**
 * Abstraction over "how a notification gets handed off for async processing".
 * The service layer depends only on this interface (DIP) — swapping Kafka
 * for an in-memory queue (the spec's stated fallback) means adding one new
 * implementation, not touching NotificationService.
 */
public interface NotificationQueuePublisher {

    void publish(NotificationQueueMessage message);
}
