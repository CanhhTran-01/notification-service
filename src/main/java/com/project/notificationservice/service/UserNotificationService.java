package com.project.notificationservice.service;

import com.project.notificationservice.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserNotificationService {

    Page<NotificationResponse> getHistory(String recipientId, Pageable pageable);

    void updateRead(String notificationId, String recipientId);

    Long countNotReadNotification(String recipientId);
}
