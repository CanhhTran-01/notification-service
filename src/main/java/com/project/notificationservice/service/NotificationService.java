package com.project.notificationservice.service;

import com.project.notificationservice.domain.entity.Notification;

public interface NotificationService {
    void send(Notification notification);
}
