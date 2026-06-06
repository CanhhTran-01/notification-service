package com.project.notificationservice.sender.impl;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.sender.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InAppNotificationSender implements NotificationSender {

    @Override
    public void send(Notification notification) {

        log.info("InApp notification delivered to recipientId={}", notification.getRecipientId());
    }

    @Override
    public Channel getChannel() {
        return Channel.IN_APP;
    }
}
