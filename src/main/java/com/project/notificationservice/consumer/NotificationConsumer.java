package com.project.notificationservice.consumer;

import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.dto.NotificationEvent;
import com.project.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "notification.queue")
    public void receive(NotificationEvent event) {

        for (var channel : event.getChannels()) {
            Notification notification = Notification.builder()
                    .recipientId(event.getRecipient().getUserId())
                    .recipientContact(event.getRecipient().getContactByChannel(channel))
                    .channel(channel)
                    .payload(event.getPayload())
                    .build();

            notificationService.send(notification);
        }
    }
}
