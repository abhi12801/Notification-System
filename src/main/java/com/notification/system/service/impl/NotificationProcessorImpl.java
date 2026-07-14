package com.notification.system.service.impl;

import com.notification.system.entity.Notification;
import com.notification.system.entity.NotificationAttempt;
import com.notification.system.enums.AttemptOutcome;
import com.notification.system.enums.NotificationStatus;
import com.notification.system.queue.NotificationQueueMessage;
import com.notification.system.repository.NotificationAttemptRepository;
import com.notification.system.repository.NotificationRepository;
import com.notification.system.service.NotificationProcessor;
import com.notification.system.strategy.NotificationChannelSender;
import com.notification.system.strategy.NotificationChannelSenderRegistry;
import com.notification.system.util.RandomFailureSimulator;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class NotificationProcessorImpl implements NotificationProcessor {

    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository attemptRepository;
    private final NotificationChannelSenderRegistry senderRegistry;
    private final RandomFailureSimulator failureSimulator;

    public NotificationProcessorImpl(
            NotificationRepository notificationRepository,
            NotificationAttemptRepository attemptRepository,
            NotificationChannelSenderRegistry senderRegistry,
            RandomFailureSimulator failureSimulator) {
        this.notificationRepository = notificationRepository;
        this.attemptRepository = attemptRepository;
        this.senderRegistry = senderRegistry;
        this.failureSimulator = failureSimulator;
    }

    @Override
    @Transactional
    public void process(NotificationQueueMessage message) {
        Notification notification = notificationRepository.findById(message.notificationId()).orElse(null);
        if (notification == null) {
            // Defensive only: nothing in this system deletes notifications today.
            log.warn("Skipping message for unknown notification id={}", message.notificationId());
            return;
        }

        if (notification.getStatus() == NotificationStatus.SENT) {
            // Kafka gives at-least-once delivery: a redelivered message must be a no-op,
            // not a duplicate send to the user.
            log.info("Notification id={} already SENT, ignoring redelivered message", notification.getId());
            return;
        }

        NotificationChannelSender sender = senderRegistry.resolve(notification.getType());
        sender.send(notification);

        boolean failed = failureSimulator.shouldFail();
        int attemptNumber = notification.getRetryCount() + 1;
        LocalDateTime now = LocalDateTime.now();

        notification.setLastAttemptedAt(now);
        notification.setStatus(failed ? NotificationStatus.FAILED : NotificationStatus.SENT);
        if (!failed) {
            notification.setProcessedAt(now);
        }
        notificationRepository.save(notification);

        NotificationAttempt attempt = NotificationAttempt.builder()
                .notification(notification)
                .attemptNumber(attemptNumber)
                .outcome(failed ? AttemptOutcome.FAILED : AttemptOutcome.SENT)
                .failureReason(failed ? "Simulated transient failure from downstream channel" : null)
                .triggeredBy(message.triggeredBy())
                .build();
        attemptRepository.save(attempt);

        log.info("Processed notification id={} attempt=#{} trigger={} outcome={}",
                notification.getId(), attemptNumber, message.triggeredBy(), notification.getStatus());
    }
}
