package com.notification.system.repository;

import com.notification.system.entity.Notification;
import com.notification.system.enums.NotificationStatus;
import com.notification.system.enums.NotificationType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds optional, composable filter predicates for GET /api/notifications.
 * Specifications avoid a combinatorial explosion of derived query methods
 * (findByStatus, findByType, findByStatusAndType, ...) for what is really
 * just "AND together whichever filters were supplied".
 */
public final class NotificationSpecifications {

    private NotificationSpecifications() {
    }

    public static Specification<Notification> hasStatus(NotificationStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Notification> hasType(NotificationType type) {
        return (root, query, cb) ->
                type == null ? null : cb.equal(root.get("type"), type);
    }
}
