package com.project.notificationservice.service;

public interface AdminNotificationService {
    void retry(String messageId);

    void replay(String messageId, String queueName);
}
