package com.notification.system.repository;

import com.notification.system.entity.Notification;
import com.notification.system.enums.NotificationType;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * JpaSpecificationExecutor backs the Module 7 list API, where status/type
 * filters are optional and combine freely — Specifications avoid a
 * combinatorial explosion of derived query methods (one per filter combo).
 */
public interface NotificationRepository
        extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    /**
     * Backs the duplicate-notification business rule: same user, same type,
     * same message, within a 5-minute window. {@code createdSince} is the
     * caller-computed lower bound (now - 5 minutes).
     */
    boolean existsByUserIdAndTypeAndMessageAndCreatedAtAfter(
            Long userId, NotificationType type, String message, LocalDateTime createdSince);
}
