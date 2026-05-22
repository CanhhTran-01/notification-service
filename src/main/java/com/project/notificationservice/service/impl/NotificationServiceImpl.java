package com.project.notificationservice.service.impl;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.entity.NotificationLog;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.domain.enums.NotificationStatus;
import com.project.notificationservice.exception.BaseException;
import com.project.notificationservice.exception.ErrorCode;
import com.project.notificationservice.repository.NotificationLogRepository;
import com.project.notificationservice.repository.NotificationRepository;
import com.project.notificationservice.sender.NotificationSender;
import com.project.notificationservice.service.NotificationService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final Map<Channel, NotificationSender> senderMap;
    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;

    public NotificationServiceImpl(
            List<NotificationSender> senders,
            NotificationRepository notificationRepository,
            NotificationLogRepository notificationLogRepository) {

        this.senderMap = senders.stream().collect(Collectors.toMap(NotificationSender::getChannel, sender -> sender));
        this.notificationRepository = notificationRepository;
        this.notificationLogRepository = notificationLogRepository;
    }

    @Async("notificationExecutor")
    @Transactional
    @Override
    public void send(Notification notification) {

        // idempotency checking
        if (notificationRepository.existsByEventId(notification.getEventId())) {
            log.warn("Duplicate event detected, skipping: {}", notification.getEventId());
            return;
        }

        // channel checking
        NotificationSender sender = senderMap.get(notification.getChannel());
        if (sender == null) {
            throw new BaseException(ErrorCode.UNSUPPORTED_CHANNEL);
        }

        // start processing
        NotificationStatus oldStatus = notification.getStatus(); // PENDING

        notification.markAsProcessing();
        notificationRepository.save(notification); // PROCESSING

        logging(notification, oldStatus, notification.getStatus(), "processing started");

        try {
            oldStatus = notification.getStatus(); // PROCESSING

            sender.send(notification);

            notification.markAsSent(); // SENT
            notificationRepository.save(notification);

            logging(notification, oldStatus, notification.getStatus(), "sent successfully");

        } catch (Exception exception) {

            oldStatus = notification.getStatus(); // PROCESSING

            notification.incrementRetry(); // PENDING -> Scheduled Job scan DB for retrying
            notificationRepository.save(notification);

            logging(notification, oldStatus, notification.getStatus(), exception.getMessage());

            // FAILED
            if (notification.getStatus() == NotificationStatus.FAILED) {
                throw new AmqpRejectAndDontRequeueException(exception); // chấp nhận bị executor nuốt - fail silently
            }
        }
    }

    // handle logging notification
    private void logging(
            Notification notification, NotificationStatus oldStatus, NotificationStatus newStatus, String message) {

        notificationLogRepository.save(NotificationLog.builder()
                .notification(notification)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .message(message)
                .build());
    }
}
