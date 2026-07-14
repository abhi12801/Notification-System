package com.notification.system.validation;

import com.notification.system.config.NotificationProperties;
import com.notification.system.entity.Notification;
import com.notification.system.enums.NotificationStatus;
import com.notification.system.exception.RetryNotAllowedException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Encapsulates the three retry-eligibility rules from the spec so
 * NotificationServiceImpl doesn't carry business-rule detail inline.
 * Each rule fails with a distinct, actionable message rather than one
 * generic "retry not allowed".
 */
@Component
public class RetryEligibilityValidator {

    private final NotificationProperties properties;

    public RetryEligibilityValidator(NotificationProperties properties) {
        this.properties = properties;
    }

    public void validate(Notification notification) {
        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new RetryNotAllowedException(
                    "Only FAILED notifications can be retried. Current status: " + notification.getStatus());
        }

        if (notification.getRetryCount() >= properties.getMaxRetryCount()) {
            throw new RetryNotAllowedException(
                    "Maximum retry attempts (%d) already reached".formatted(properties.getMaxRetryCount()));
        }

        LocalDateTime lastAttemptedAt = notification.getLastAttemptedAt();
        if (lastAttemptedAt != null) {
            long minutesSinceLastAttempt = ChronoUnit.MINUTES.between(lastAttemptedAt, LocalDateTime.now());
            if (minutesSinceLastAttempt < properties.getRetryCooldownMinutes()) {
                throw new RetryNotAllowedException(
                        "Retry allowed only after %d minutes since the last attempt; %d minute(s) have passed"
                                .formatted(properties.getRetryCooldownMinutes(), minutesSinceLastAttempt));
            }
        }
    }
}
