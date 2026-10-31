package com.project.notificationservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.dto.*;
import com.project.notificationservice.entity.DeadLetterEvent;
import com.project.notificationservice.entity.Notification;
import com.project.notificationservice.enums.NotificationStatus;
import com.project.notificationservice.exception.BaseException;
import com.project.notificationservice.exception.ErrorCode;
import com.project.notificationservice.properties.RetryProperties;
import com.project.notificationservice.publisher.RabbitNotificationPublisher;
import com.project.notificationservice.repository.DeadLetterEventRepository;
import com.project.notificationservice.repository.NotificationRepository;
import com.project.notificationservice.service.AdminNotificationService;
import com.project.notificationservice.specification.DeadLetterEventSpecs;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final NotificationRepository notificationRepository;
    private final DeadLetterEventRepository deadLetterEventRepository;
    private final RabbitNotificationPublisher rabbitNotificationPublisher;
    private final RetryProperties retryProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void retry(String messageId) {

        Notification notification = notificationRepository
                .findByIdAndStatus(UUID.fromString(messageId), NotificationStatus.FAILED)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.resetForRetry();
        log.info(
                "ADMIN__Notification reset for retry: id={}, eventId={}",
                notification.getId(),
                notification.getEventId());
    }

    @Override
    public void replay(String messageId, String queueName) {

        DeadLetterEvent deadLetterEvent = deadLetterEventRepository
                .findByIdAndQueueNameAndResolvedFalse(UUID.fromString(messageId), queueName)
                .orElseThrow(() -> new BaseException(ErrorCode.DEAD_LETTER_NOT_FOUND));

        // lấy ra payload
        String jsonPayload = deadLetterEvent.getPayload();

        // derialize event thành JSON
        NotificationEvent event;
        try {
            event = objectMapper.readValue(jsonPayload, NotificationEvent.class);

        } catch (JsonProcessingException e) {

            log.error("ADMIN__Failed to deserialize dead letter payload: id={}, error={}", messageId, e.getMessage());
            throw new BaseException(ErrorCode.INVALID_DEAD_LETTER_PAYLOAD);
        }

        // Kiểm tra dữ liệu trước khi replay để ngăn message không hợp lệ, gây lỗi và requeue vô tận.
        validateNotificationEvent(event, messageId);

        // publish lại vào broker
        rabbitNotificationPublisher.publish(event);

        // đánh dấu resolved
        deadLetterEvent.setResolved(true);
        log.info("ADMIN__Dead letter event replayed: id={}, eventId={}", messageId, event.getEventId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getFailedNotifications(Pageable pageable) {
        return notificationRepository
                .findByStatusOrderByCreatedAtDesc(NotificationStatus.FAILED, pageable)
                .map(this::toNotificationResponse);
    }

    @Override
    public Page<DeadLetterEventResponse> searchDeadLetterEvents(Boolean resolved, String queueName, Pageable pageable) {
        return deadLetterEventRepository
                .findAll(DeadLetterEventSpecs.withCriteria(resolved, queueName), pageable)
                .map(this::toDeadLetterResponse);
    }

    @Override
    public NotificationResponse createDraft(CreateNotificationRequest request) {

        if (notificationRepository.existsByEventIdAndChannel(request.getEventId(), request.getChannel())) {
            throw new BaseException(ErrorCode.NOTIFICATION_DRAFT_ALREADY_EXISTS);
        }

        Notification notification = Notification.builder()
                .eventId(request.getEventId())
                .source(request.getSource())
                .recipientId(request.getRecipientId())
                .recipientContact(request.getRecipientContact())
                .channel(request.getChannel())
                .eventType(request.getEventType())
                .payload(request.getPayload())
                .maxRetries(retryProperties.getMaxRetries())
                .retryCount(0)
                .status(NotificationStatus.DRAFT)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("ADMIN__Draft notification created: id={}, eventId={}", saved.getId(), saved.getEventId());

        return toNotificationResponse(saved);
    }

    @Override
    public NotificationResponse schedule(String id, ScheduleNotificationRequest request) {

        Notification notification = notificationRepository
                .findById(UUID.fromString(id))
                .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // chỉ cho lên/update lịch với các notification có status DRAFT or PENDING
        if (notification.getStatus() != NotificationStatus.DRAFT
                && notification.getStatus() != NotificationStatus.PENDING) {
            throw new BaseException(ErrorCode.NOTIFICATION_STATUS_NOT_SCHEDULABLE);
        }

        // time không hợp lệ
        if (request.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new BaseException(ErrorCode.NOTIFICATION_SCHEDULED_TIME_INVALID);
        }

        notification.scheduleAt(request.getScheduledAt());
        log.info("ADMIN__Notification scheduled: id={}, scheduledAt={}", id, request.getScheduledAt());

        return toNotificationResponse(notification);
    }

    @Override
    public NotificationResponse updateFailed(String id, UpdateFailedNotificationRequest request) {

        Notification notification = notificationRepository
                .findByIdAndStatus(UUID.fromString(id), NotificationStatus.FAILED)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.updateFailedContent(
                request.getRecipientContact(), request.getPayload(), request.getErrorMessage());

        log.info("ADMIN__Failed notification content updated for retry: id={}", id);
        return toNotificationResponse(notification);
    }

    private void validateNotificationEvent(NotificationEvent event, String messageId) {

        if (event == null) {
            log.error("Invalid NotificationEvent: id={}, reason=Event is null", messageId);
            throw new BaseException(ErrorCode.INVALID_NOTIFICATION_EVENT);
        }

        if (event.getEventId() == null
                || event.getEventId().isBlank()
                || event.getEventType() == null
                || event.getSource() == null
                || event.getTimestamp() == null
                || event.getRecipient() == null
                || event.getChannels() == null
                || event.getChannels().isEmpty()
                || event.getChannels().contains(null)
                || event.getPayload() == null) {

            log.error("Invalid NotificationEvent payload: id={}, eventId={}", messageId, event.getEventId());

            throw new BaseException(ErrorCode.INVALID_NOTIFICATION_EVENT);
        }
    }

    private NotificationResponse toNotificationResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .eventType(n.getEventType())
                .isRead(n.isRead())
                .payload(n.getPayload())
                .sentAt(n.getSentAt())
                .createdAt(n.getCreatedAt())
                .recipientId(n.getRecipientId())
                .recipientContact(n.getRecipientContact())
                .channel(n.getChannel())
                .status(n.getStatus())
                .errorMessage(n.getErrorMessage())
                .retryCount(n.getRetryCount())
                .scheduledAt(n.getScheduledAt())
                .nextRetryTime(n.getNextRetryTime())
                .build();
    }

    private DeadLetterEventResponse toDeadLetterResponse(DeadLetterEvent e) {
        return DeadLetterEventResponse.builder()
                .id(e.getId())
                .queueName(e.getQueueName())
                .payload(e.getPayload())
                .errorReason(e.getErrorReason())
                .retryCount(e.getRetryCount())
                .resolved(e.isResolved())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
