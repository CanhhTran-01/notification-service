package com.project.notificationservice.service;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    void send(Notification notification);

    Page<NotificationResponse> getHistory(String recipientId, Pageable pageable);

    void updateRead(String notificationId, String recipientId);

    Long countNotReadNotification(String recipientId);
}
