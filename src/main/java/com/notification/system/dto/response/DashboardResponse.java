package com.notification.system.dto.response;

import com.notification.system.enums.NotificationType;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardResponse {

    private long totalNotifications;
    private long sentCount;
    private long failedCount;
    private long pendingCount;
    private long retryingCount;
    private Map<NotificationType, Long> typeWiseStats;
}
