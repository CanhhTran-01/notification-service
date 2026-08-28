package com.project.notificationservice.service;

import com.project.notificationservice.entity.Notification;

public interface NotificationStatusService {
    void markAsProcessing(Notification notification);

    void markAsSent(Notification notification);

    void markAsRetryOrFailed(Notification notificatio, Exception exception);

    void handleBusinessFailure(Notification notification, String errorMessage);

    void markAsRetrying(Notification notification);
}
