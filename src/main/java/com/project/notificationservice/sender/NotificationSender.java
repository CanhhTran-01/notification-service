package com.project.notificationservice.sender;

import com.project.notificationservice.entity.Notification;
import com.project.notificationservice.enums.Channel;

public interface NotificationSender {

    void send(Notification notification);

    Channel getChannel();
}
