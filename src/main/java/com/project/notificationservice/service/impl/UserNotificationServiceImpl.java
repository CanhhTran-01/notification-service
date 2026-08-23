package com.project.notificationservice.service.impl;

import com.project.notificationservice.dto.NotificationResponse;
import com.project.notificationservice.entity.Notification;
import com.project.notificationservice.enums.Channel;
import com.project.notificationservice.exception.BaseException;
import com.project.notificationservice.exception.ErrorCode;
import com.project.notificationservice.repository.NotificationRepository;
import com.project.notificationservice.service.UserNotificationService;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

    private final NotificationRepository notificationRepository;

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
