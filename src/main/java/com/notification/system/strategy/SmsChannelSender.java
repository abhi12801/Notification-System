package com.notification.system.strategy;

import com.notification.system.entity.Notification;
import com.notification.system.enums.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsChannelSender implements NotificationChannelSender {

    @Override
    public NotificationType getSupportedType() {
        return NotificationType.SMS;
    }

    @Override
    public void send(Notification notification) {
        log.info("[SMS] Dispatching to userId={} -> \"{}\"", notification.getUserId(), notification.getMessage());
    }
}
