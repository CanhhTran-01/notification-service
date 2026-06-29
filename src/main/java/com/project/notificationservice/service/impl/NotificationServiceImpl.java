package com.project.notificationservice.service.impl;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.entity.NotificationLog;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.domain.enums.CheckingGatewayType;
import com.project.notificationservice.domain.enums.NotificationStatus;
import com.project.notificationservice.dto.NotificationResponse;
import com.project.notificationservice.exception.BaseException;
import com.project.notificationservice.exception.ErrorCode;
import com.project.notificationservice.repository.NotificationLogRepository;
import com.project.notificationservice.repository.NotificationRepository;
import com.project.notificationservice.sender.NotificationSender;
import com.project.notificationservice.service.NotificationService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        // checking gateway 1: idempotency checking
        if (notificationRepository.existsByEventIdAndChannel(notification.getEventId(), notification.getChannel())) {
            log.warn(
                    "Duplicate event detected, skipping: eventId [{}] with channel [{}]",
                    notification.getEventId(),
                    notification.getChannel());
            return;
        }

        // checking gateway 2: channel checking
        NotificationSender sender = senderMap.get(notification.getChannel());
        if (sender == null) {
            handleBusinessFailure(
                    notification,
                    "Unsupported channel: " + notification.getChannel(),
                    CheckingGatewayType.CHANNEL_CHECKING);
            return;
        }

        // checking gateway 3: recipient_contact checking
        if (notification.getRecipientContact() == null) {
            handleBusinessFailure(
                    notification,
                    "Missing contact info for channel: " + notification.getChannel(),
                    CheckingGatewayType.RECIPIENT_CONTACT_CHECKING);
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
            notification.incrementRetryAndCalculateNextTime(60); // PENDING

            notificationRepository.save(notification);

            // >= maxRetries -> FAILED
            logging(notification, oldStatus, notification.getStatus(), exception.getMessage());
        }
    }

    @Override
    public Page<NotificationResponse> getHistory(String recipientId, Pageable pageable) {

        Page<Notification> pageList = notificationRepository.findByRecipientIdAndChannelOrderByCreatedAtDesc(
                recipientId, Channel.IN_APP, pageable);

        return pageList.map(this::toResponse);
    }

    @Override
    @Transactional
    public void updateRead(String notificationId, String recipientId) {

        // tránh IDOR
        Notification notification = notificationRepository
                .findByIdAndRecipientId(UUID.fromString(notificationId), recipientId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
    }

    @Override
    public Long countNotReadNotification(String recipientId) {

        return notificationRepository.countByRecipientIdAndChannelAndIsReadFalse(recipientId, Channel.IN_APP);
    }

    // handle business failure
    private void handleBusinessFailure(Notification notification, String errorMessage, CheckingGatewayType type) {

        // System Log cho Dev/DevOps xem trên Console/Kibana
        log.warn(
                "Cannot process eventId [{}] with channel [{}]: {}",
                notification.getEventId(),
                notification.getChannel(),
                errorMessage);

        // Lưu notification FAILED vào DB phục vụ retry sau này
        notification.cancelling(errorMessage, type); // set status FAILED + errorMessage
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

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .eventType(notification.getEventType())
                .isRead(notification.isRead())
                .payload(notification.getPayload())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
