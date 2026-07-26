package com.project.notificationservice.sender;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.enums.Channel;

public interface NotificationSender {

    void send(Notification notification);

    Channel getChannel();
}
