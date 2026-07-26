package com.project.notificationservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.domain.entity.DeadLetterEvent;
import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.enums.NotificationStatus;
import com.project.notificationservice.dto.NotificationEvent;
import com.project.notificationservice.exception.BaseException;
import com.project.notificationservice.exception.ErrorCode;
import com.project.notificationservice.mock.RabbitNotificationPublisher;
import com.project.notificationservice.repository.DeadLetterEventRepository;
import com.project.notificationservice.repository.NotificationRepository;
import com.project.notificationservice.service.AdminNotificationService;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final NotificationRepository notificationRepository;
    private final DeadLetterEventRepository deadLetterEventRepository;
    private final RabbitNotificationPublisher rabbitNotificationPublisher;
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

        // publish lại vào broker
        rabbitNotificationPublisher.publish(event);

        // đánh dấu resolved
        deadLetterEvent.setResolved(true);
        log.info("ADMIN__Dead letter event replayed: id={}, eventId={}", messageId, event.getEventId());
    }
}
