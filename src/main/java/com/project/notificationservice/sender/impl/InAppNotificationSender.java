package com.project.notificationservice.sender.impl;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.sender.NotificationSender;
import com.project.notificationservice.service.SseEmitterService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationSender implements NotificationSender {

    private final SseEmitterService sseEmitterService;

    @Override
    public void send(Notification notification) {

        Map<String, Object> ssePayload = Map.of(
                "id", notification.getId(),
                "eventType", notification.getEventType(),
                "payload", notification.getPayload(),
                "createdAt", notification.getCreatedAt());

        sseEmitterService.sendToUser(notification.getRecipientId(), ssePayload);
    }

    @Override
    public Channel getChannel() {
        return Channel.IN_APP;
    }
}
