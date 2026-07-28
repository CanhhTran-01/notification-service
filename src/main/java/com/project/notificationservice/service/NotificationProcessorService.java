package com.project.notificationservice.service;

import com.project.notificationservice.domain.entity.Notification;

public interface NotificationProcessorService {
    void send(Notification notification); // EMAIL-SMS-PUSH Channel dùng @Async

    void sendInApp(Notification notification); // IN_APP Channel dùng RabbitMQ concurrency
}
