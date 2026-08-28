package com.project.notificationservice.service;

import com.project.notificationservice.entity.Notification;

public interface NotificationProcessorService {
    void process(Notification notification);

    void sendExternal(Notification notification); // EMAIL-SMS-PUSH Channel dùng @Async

    void sendInApp(Notification notification); // IN_APP Channel dùng RabbitMQ concurrency
}
