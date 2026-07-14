package com.notification.system.strategy;

import com.notification.system.entity.Notification;
import com.notification.system.enums.NotificationType;

/**
 * One implementation per {@link NotificationType}. Adding a 4th channel
 * means adding one new implementation (OCP) — no switch-case in the
 * processor to touch, no existing sender to risk breaking.
 */
public interface NotificationChannelSender {

    NotificationType getSupportedType();

    /** Simulates dispatch to the external channel (logging only — no real integration in scope). */
    void send(Notification notification);
}
