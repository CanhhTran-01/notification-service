package com.project.notificationservice.service;

import com.project.notificationservice.domain.entity.Notification;

public interface NotificationProcessorService {
    void send(Notification notification);
}
