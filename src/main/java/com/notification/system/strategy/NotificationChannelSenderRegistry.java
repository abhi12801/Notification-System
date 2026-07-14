package com.notification.system.strategy;

import com.notification.system.enums.NotificationType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Builds the type -> sender lookup once at startup from whatever
 * {@link NotificationChannelSender} beans Spring finds. This is what makes
 * the design open for extension: a new sender class registers itself just by
 * existing as a bean, with no if/switch anywhere needing an edit.
 */
@Component
public class NotificationChannelSenderRegistry {

    private final Map<NotificationType, NotificationChannelSender> sendersByType;

    public NotificationChannelSenderRegistry(List<NotificationChannelSender> senders) {
        this.sendersByType = senders.stream()
                .collect(Collectors.toMap(NotificationChannelSender::getSupportedType, Function.identity()));
    }

    public NotificationChannelSender resolve(NotificationType type) {
        NotificationChannelSender sender = sendersByType.get(type);
        if (sender == null) {
            throw new IllegalStateException("No NotificationChannelSender registered for type: " + type);
        }
        return sender;
    }
}
