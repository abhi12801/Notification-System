package com.notification.system.service.impl;

import com.notification.system.config.NotificationProperties;
import com.notification.system.dto.request.CreateNotificationRequest;
import com.notification.system.dto.response.NotificationResponse;
import com.notification.system.entity.Notification;
import com.notification.system.enums.AttemptTrigger;
import com.notification.system.enums.NotificationStatus;
import com.notification.system.enums.NotificationType;
import com.notification.system.exception.DuplicateNotificationException;
import com.notification.system.exception.NotificationNotFoundException;
import com.notification.system.mapper.NotificationMapper;
import com.notification.system.queue.NotificationQueueMessage;
import com.notification.system.queue.NotificationQueuePublisher;
import com.notification.system.repository.NotificationRepository;
import com.notification.system.repository.NotificationSpecifications;
import com.notification.system.service.NotificationService;
import com.notification.system.validation.RetryEligibilityValidator;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationQueuePublisher queuePublisher;
    private final RetryEligibilityValidator retryEligibilityValidator;
    private final NotificationProperties properties;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper,
            NotificationQueuePublisher queuePublisher,
            RetryEligibilityValidator retryEligibilityValidator,
            NotificationProperties properties) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.queuePublisher = queuePublisher;
        this.retryEligibilityValidator = retryEligibilityValidator;
        this.properties = properties;
    }

    @Override
    @Transactional
    public NotificationResponse create(CreateNotificationRequest request) {
        rejectIfDuplicate(request);

        Notification notification = notificationMapper.toEntity(request);
        Notification saved = notificationRepository.save(notification);

        queuePublisher.publish(new NotificationQueueMessage(saved.getId(), AttemptTrigger.INITIAL));
        log.info("Created notification id={} userId={} type={}, queued for processing",
                saved.getId(), saved.getUserId(), saved.getType());

        return notificationMapper.toResponse(saved);
    }

    @Override
    public NotificationResponse getById(Long id) {
        return notificationMapper.toResponse(findOrThrow(id));
    }

    @Override
    public Page<NotificationResponse> list(NotificationStatus status, NotificationType type, Pageable pageable) {
        Specification<Notification> spec = Specification
                .where(NotificationSpecifications.hasStatus(status))
                .and(NotificationSpecifications.hasType(type));

        return notificationRepository.findAll(spec, pageable).map(notificationMapper::toResponse);
    }

    @Override
    @Transactional
    public NotificationResponse retry(Long id) {
        Notification notification = findOrThrow(id);
        retryEligibilityValidator.validate(notification);

        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setStatus(NotificationStatus.RETRYING);
        notification.setLastAttemptedAt(LocalDateTime.now());
        Notification saved = notificationRepository.save(notification);

        queuePublisher.publish(new NotificationQueueMessage(saved.getId(), AttemptTrigger.MANUAL_RETRY));
        log.info("Retry #{} accepted for notification id={}, queued for processing",
                saved.getRetryCount(), saved.getId());

        return notificationMapper.toResponse(saved);
    }

    private void rejectIfDuplicate(CreateNotificationRequest request) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(properties.getDuplicateWindowMinutes());
        boolean duplicate = notificationRepository.existsByUserIdAndTypeAndMessageAndCreatedAtAfter(
                request.getUserId(), request.getType(), request.getMessage(), windowStart);

        if (duplicate) {
            throw new DuplicateNotificationException(
                    "An identical notification for this user was already created within the last %d minutes"
                            .formatted(properties.getDuplicateWindowMinutes()));
        }
    }

    private Notification findOrThrow(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }
}
