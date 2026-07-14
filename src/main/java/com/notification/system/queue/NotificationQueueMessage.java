package com.notification.system.queue;

import com.notification.system.enums.AttemptTrigger;

/**
 * Queue payload carries only the notification id + trigger reason — the
 * consumer re-reads current state from the database rather than trusting a
 * possibly-stale snapshot carried in the message. Keeps the database as the
 * single source of truth.
 */
public record NotificationQueueMessage(Long notificationId, AttemptTrigger triggeredBy) {
}
