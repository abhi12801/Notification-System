package com.notification.system.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.notification.system.config.NotificationProperties;
import com.notification.system.entity.Notification;
import com.notification.system.enums.NotificationStatus;
import com.notification.system.enums.NotificationType;
import com.notification.system.exception.RetryNotAllowedException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RetryEligibilityValidatorTest {

    private RetryEligibilityValidator validator;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties();
        properties.setMaxRetryCount(3);
        properties.setRetryCooldownMinutes(2);
        validator = new RetryEligibilityValidator(properties);
    }

    @Test
    void rejectsRetryWhenStatusIsNotFailed() {
        Notification notification = notification(NotificationStatus.PENDING, 0, null);

        assertThatThrownBy(() -> validator.validate(notification))
                .isInstanceOf(RetryNotAllowedException.class)
                .hasMessageContaining("Only FAILED notifications");
    }

    @Test
    void rejectsRetryWhenMaxRetryCountReached() {
        Notification notification = notification(NotificationStatus.FAILED, 3, LocalDateTime.now().minusMinutes(10));

        assertThatThrownBy(() -> validator.validate(notification))
                .isInstanceOf(RetryNotAllowedException.class)
                .hasMessageContaining("Maximum retry attempts");
    }

    @Test
    void rejectsRetryWithinCooldownWindow() {
        Notification notification = notification(NotificationStatus.FAILED, 1, LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> validator.validate(notification))
                .isInstanceOf(RetryNotAllowedException.class)
                .hasMessageContaining("Retry allowed only after");
    }

    @Test
    void allowsRetryWhenAllThreeConditionsAreSatisfied() {
        Notification notification = notification(NotificationStatus.FAILED, 1, LocalDateTime.now().minusMinutes(5));

        assertThatCode(() -> validator.validate(notification)).doesNotThrowAnyException();
    }

    private Notification notification(NotificationStatus status, int retryCount, LocalDateTime lastAttemptedAt) {
        return Notification.builder()
                .id(1L)
                .userId(101L)
                .type(NotificationType.EMAIL)
                .message("Welcome")
                .status(status)
                .retryCount(retryCount)
                .lastAttemptedAt(lastAttemptedAt)
                .build();
    }
}
