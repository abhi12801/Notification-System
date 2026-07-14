package com.notification.system.repository;

import com.notification.system.entity.NotificationAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationAttemptRepository extends JpaRepository<NotificationAttempt, Long> {

    List<NotificationAttempt> findByNotificationIdOrderByAttemptNumberAsc(Long notificationId);
}
