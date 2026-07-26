package com.project.notificationservice.service.impl;

import com.project.notificationservice.config.properties.RetryProperties;
import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.entity.NotificationLog;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.domain.enums.NotificationStatus;
import com.project.notificationservice.repository.NotificationLogRepository;
import com.project.notificationservice.repository.NotificationRepository;
import com.project.notificationservice.sender.NotificationSender;
import com.project.notificationservice.service.NotificationProcessorService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationProcessorServiceImpl implements NotificationProcessorService {

    private final Map<Channel, NotificationSender> senderMap;
    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final RetryProperties retryProperties;

    public NotificationProcessorServiceImpl(
            List<NotificationSender> senders,
            NotificationRepository notificationRepository,
            NotificationLogRepository notificationLogRepository, RetryProperties retryProperties) {

        this.senderMap = senders.stream().collect(Collectors.toMap(NotificationSender::getChannel, sender -> sender));
        this.notificationRepository = notificationRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.retryProperties = retryProperties;
    }

    @Async("notificationExecutor")
    @Transactional
    @Override
    public void send(Notification notification) {

        // checking gateway 1: idempotency checking
        if (notification.getId() == null) {
            // CHƯA CÓ ID -> message lần đầu được gửi đi -> cần idempotency checking
            if (notificationRepository.existsByEventIdAndChannel(
                    notification.getEventId(), notification.getChannel())) {
                log.warn(
                        "Duplicate event detected, skipping: eventId [{}] with channel [{}]",
                        notification.getEventId(),
                        notification.getChannel());
                return;
            }
        } else {
            // ĐÃ CÓ ID -> là tiến trình Retry lấy từ DB lên -> bỏ qua idempotency checking
            log.info("Processing retry for notification ID: [{}]", notification.getId());
        }

        // checking gateway 2: channel checking
        NotificationSender sender = senderMap.get(notification.getChannel());
        if (sender == null) {
            handleBusinessFailure(notification, "Unsupported channel: " + notification.getChannel());
            return;
        }

        // checking gateway 3: recipient_contact checking
        if (notification.getRecipientContact() == null) {
            handleBusinessFailure(notification, "Missing contact info for channel: " + notification.getChannel());
            return;
        }

        // vượt qua hết 3 cổng checking -> start processing
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

            // Scheduled Job scan DB for retrying with exponential backoff
            notification.incrementRetryAndCalculateNextTime(retryProperties.getBaseDelaySeconds()); // PENDING

            notificationRepository.save(notification);

            // >= maxRetries -> FAILED
            logging(notification, oldStatus, notification.getStatus(), exception.getMessage());
        }
    }

    // handle business failure
    private void handleBusinessFailure(Notification notification, String errorMessage) {

        // System Log cho Dev/DevOps xem trên Console/Kibana
        log.warn(
                "Cannot process eventId [{}] with channel [{}]: {}",
                notification.getEventId(),
                notification.getChannel(),
                errorMessage);

        // Lưu notification FAILED vào DB phục vụ retry sau này
        notification.cancelling(errorMessage); // set status FAILED + errorMessage
        notificationRepository.save(notification);

        // Business Log tạo Audit Trail cho Admin
        logging(notification, NotificationStatus.UNKNOWN, NotificationStatus.CANCELLED, errorMessage);
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
