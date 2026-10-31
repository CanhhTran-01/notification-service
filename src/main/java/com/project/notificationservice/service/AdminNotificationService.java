package com.project.notificationservice.service;

import com.project.notificationservice.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminNotificationService {
    void retry(String messageId);

    void replay(String messageId, String queueName);

    Page<NotificationResponse> getFailedNotifications(Pageable pageable);

    public Page<DeadLetterEventResponse> searchDeadLetterEvents(Boolean resolved, String queueName, Pageable pageable);

    NotificationResponse createDraft(CreateNotificationRequest request);

    NotificationResponse schedule(String id, ScheduleNotificationRequest request);

    NotificationResponse updateFailed(String id, UpdateFailedNotificationRequest request);
}
