package com.notification.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notification.system.config.NotificationProperties;
import com.notification.system.dto.request.CreateNotificationRequest;
import com.notification.system.dto.response.NotificationResponse;
import com.notification.system.entity.Notification;
import com.notification.system.enums.NotificationStatus;
import com.notification.system.enums.NotificationType;
import com.notification.system.exception.DuplicateNotificationException;
import com.notification.system.mapper.NotificationMapper;
import com.notification.system.queue.NotificationQueueMessage;
import com.notification.system.queue.NotificationQueuePublisher;
import com.notification.system.repository.NotificationRepository;
import com.notification.system.service.impl.NotificationServiceImpl;
import com.notification.system.validation.RetryEligibilityValidator;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMapper notificationMapper;
    @Mock private NotificationQueuePublisher queuePublisher;
    @Mock private RetryEligibilityValidator retryEligibilityValidator;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties();
        properties.setDuplicateWindowMinutes(5);
        service = new NotificationServiceImpl(
                notificationRepository, notificationMapper, queuePublisher, retryEligibilityValidator, properties);
    }

    @Test
    void rejectsCreateWhenDuplicateExistsWithinWindow() {
        CreateNotificationRequest request = request();
        when(notificationRepository.existsByUserIdAndTypeAndMessageAndCreatedAtAfter(
                eq(101L), eq(NotificationType.EMAIL), eq("Welcome User"), any(LocalDateTime.class)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateNotificationException.class);

        verify(notificationRepository, never()).save(any());
        verify(queuePublisher, never()).publish(any());
    }

    @Test
    void savesAndPublishesOnSuccessfulCreate() {
        CreateNotificationRequest request = request();
        Notification entity = Notification.builder().userId(101L).type(NotificationType.EMAIL)
                .message("Welcome User").status(NotificationStatus.PENDING).retryCount(0).build();
        Notification saved = Notification.builder().id(42L).userId(101L).type(NotificationType.EMAIL)
                .message("Welcome User").status(NotificationStatus.PENDING).retryCount(0).build();

        when(notificationRepository.existsByUserIdAndTypeAndMessageAndCreatedAtAfter(
                anyLong(), any(), any(), any(LocalDateTime.class))).thenReturn(false);
        when(notificationMapper.toEntity(request)).thenReturn(entity);
        when(notificationRepository.save(entity)).thenReturn(saved);
        when(notificationMapper.toResponse(saved)).thenReturn(
                NotificationResponse.builder().id(42L).userId(101L).build());

        NotificationResponse response = service.create(request);

        assertThat(response.getId()).isEqualTo(42L);
        verify(queuePublisher, times(1)).publish(any(NotificationQueueMessage.class));
    }

    private CreateNotificationRequest request() {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(101L);
        request.setType(NotificationType.EMAIL);
        request.setMessage("Welcome User");
        return request;
    }
}
