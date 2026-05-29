package com.project.notificationservice.sender.impl;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.sender.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PushNotificationSender implements NotificationSender {

    @Override
    public void send(Notification notification) {

        // TODO: integrate with FCM (Firebase)
        log.info("send PUSH to {} with PAYLOAD {}", notification.getRecipientContact(), notification.getPayload());
    }

    @Override
    public Channel getChannel() {
        return Channel.PUSH;
    }
}
