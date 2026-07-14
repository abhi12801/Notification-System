package com.notification.system.service;

import com.notification.system.queue.NotificationQueueMessage;

/** Performs the actual "send" for a queued notification — invoked by the Kafka consumer. */
public interface NotificationProcessor {

    void process(NotificationQueueMessage message);
}
