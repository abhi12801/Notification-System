package com.notification.system.service.impl;

import com.notification.system.dto.response.DashboardResponse;
import com.notification.system.enums.NotificationStatus;
import com.notification.system.enums.NotificationType;
import com.notification.system.repository.NotificationRepository;
import com.notification.system.repository.NotificationRepository.StatusCountProjection;
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
        Map<NotificationStatus, Long> statusCounts = notificationRepository.countGroupedByStatus().stream()
                .collect(Collectors.toMap(StatusCountProjection::getStatus, StatusCountProjection::getCount));

        Map<NotificationType, Long> typeWiseStats = notificationRepository.countGroupedByType().stream()
                .collect(Collectors.toMap(TypeCountProjection::getType, TypeCountProjection::getCount));

        // Every notification has exactly one status, so summing the group counts
        // equals the total row count -- no separate count(*) round trip needed.
        long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();

        return DashboardResponse.builder()
                .totalNotifications(total)
                .sentCount(statusCounts.getOrDefault(NotificationStatus.SENT, 0L))
                .failedCount(statusCounts.getOrDefault(NotificationStatus.FAILED, 0L))
                .pendingCount(statusCounts.getOrDefault(NotificationStatus.PENDING, 0L))
                .retryingCount(statusCounts.getOrDefault(NotificationStatus.RETRYING, 0L))
                .typeWiseStats(typeWiseStats)
                .build();
    }
}
