package com.notification.system.service.impl;

import com.notification.system.dto.response.DashboardResponse;
import com.notification.system.enums.NotificationStatus;
import com.notification.system.enums.NotificationType;
import com.notification.system.repository.NotificationRepository;
import com.notification.system.repository.NotificationRepository.TypeCountProjection;
import com.notification.system.service.DashboardService;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final NotificationRepository notificationRepository;

    public DashboardServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public DashboardResponse getStatistics() {
        Map<NotificationType, Long> typeWiseStats = notificationRepository.countGroupedByType().stream()
                .collect(Collectors.toMap(TypeCountProjection::getType, TypeCountProjection::getCount));

        return DashboardResponse.builder()
                .totalNotifications(notificationRepository.count())
                .sentCount(notificationRepository.countByStatus(NotificationStatus.SENT))
                .failedCount(notificationRepository.countByStatus(NotificationStatus.FAILED))
                .pendingCount(notificationRepository.countByStatus(NotificationStatus.PENDING))
                .retryingCount(notificationRepository.countByStatus(NotificationStatus.RETRYING))
                .typeWiseStats(typeWiseStats)
                .build();
    }
}
