package com.notification.system.dto.response;

import com.notification.system.enums.NotificationStatus;
import com.notification.system.enums.NotificationType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponse {

    private Long id;
    private Long userId;
    private NotificationType type;
    private String message;
    private NotificationStatus status;
    private LocalDateTime scheduleTime;
    private Integer retryCount;
    private LocalDateTime lastAttemptedAt;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
