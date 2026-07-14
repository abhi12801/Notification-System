package com.notification.system.enums;

/** Distinguishes the original Kafka-driven send attempt from a manually-triggered retry. */
public enum AttemptTrigger {
    INITIAL,
    MANUAL_RETRY
}
