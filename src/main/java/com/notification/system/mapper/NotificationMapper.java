package com.notification.system.mapper;

import com.notification.system.dto.request.CreateNotificationRequest;
import com.notification.system.dto.response.NotificationResponse;
import com.notification.system.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Compile-time generated mapping (no reflection at runtime, mapping errors
 * caught at build time instead of in production).
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "retryCount", constant = "0")
    @Mapping(target = "lastAttemptedAt", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Notification toEntity(CreateNotificationRequest request);

    NotificationResponse toResponse(Notification notification);
}
