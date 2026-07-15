package com.notification.system.mapper;

import com.notification.system.dto.request.CreateNotificationRequest;
import com.notification.system.dto.response.NotificationResponse;
import com.notification.system.entity.Notification;
import com.notification.system.enums.NotificationStatus;
import org.springframework.stereotype.Component;

/**
 * Plain, hand-written entity <-> DTO mapping using the entities' existing
 * Lombok builders. No codegen, no annotation-processor step -- for a
 * two-DTO surface, a manual mapper is simpler to read and debug than
 * configuring a mapping framework.
 */
@Component
public class NotificationMapper {

    public Notification toEntity(CreateNotificationRequest request) {
        return Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .message(request.getMessage())
                .scheduleTime(request.getScheduleTime())
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();
    }

    public NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .scheduleTime(notification.getScheduleTime())
                .retryCount(notification.getRetryCount())
                .lastAttemptedAt(notification.getLastAttemptedAt())
                .processedAt(notification.getProcessedAt())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
