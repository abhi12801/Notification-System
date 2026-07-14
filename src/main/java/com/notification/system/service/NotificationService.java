package com.notification.system.service;

import com.notification.system.dto.request.CreateNotificationRequest;
import com.notification.system.dto.response.NotificationResponse;
import com.notification.system.enums.NotificationStatus;
import com.notification.system.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    NotificationResponse create(CreateNotificationRequest request);

    NotificationResponse getById(Long id);

    Page<NotificationResponse> list(NotificationStatus status, NotificationType type, Pageable pageable);

    NotificationResponse retry(Long id);
}
