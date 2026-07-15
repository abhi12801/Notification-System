package com.notification.system.repository;

import com.notification.system.entity.Notification;
import com.notification.system.enums.NotificationStatus;
import com.notification.system.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Backs the duplicate-notification business rule: same user, same type,
     * same message, within a 5-minute window. {@code createdSince} is the
     * caller-computed lower bound (now - 5 minutes).
     */
    boolean existsByUserIdAndTypeAndMessageAndCreatedAtAfter(
            Long userId, NotificationType type, String message, LocalDateTime createdSince);

    /**
     * Backs GET /api/notifications. status/type are optional — a null
     * parameter makes its (:param IS NULL OR ...) clause a no-op, so this
     * one query serves all four filter combinations (none/status/type/both).
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE (:status IS NULL OR n.status = :status)
              AND (:type IS NULL OR n.type = :type)
            """)
    Page<Notification> findByOptionalStatusAndType(
            @Param("status") NotificationStatus status,
            @Param("type") NotificationType type,
            Pageable pageable);

    /** Backs the dashboard's per-status counts; each call is a single index-only scan. */
    long countByStatus(NotificationStatus status);

    @Query("select n.type as type, count(n) as count from Notification n group by n.type")
    List<TypeCountProjection> countGroupedByType();

    interface TypeCountProjection {
        NotificationType getType();
        Long getCount();
    }
}
