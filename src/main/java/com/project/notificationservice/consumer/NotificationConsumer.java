package com.project.notificationservice.consumer;

import com.project.notificationservice.config.RabbitMQConfig;
import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.dto.NotificationEvent;
import com.project.notificationservice.service.NotificationPreferenceService;
import com.project.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void receive(NotificationEvent event) {

        for (var channel : event.getChannels()) {
            try {
                // Double-check preference trước khi xử lý
                // Không tin tưởng external service
                boolean isEnabled =
                        preferenceService.isChannelEnabled(event.getRecipient().getUserId(), channel);

                if (!isEnabled) {
                    log.info(
                            "Channel {} disabled for recipientId={}, skipping",
                            channel,
                            event.getRecipient().getUserId());
                    continue; // skip channel này, tiếp tục channel khác
                }

                Notification notification = Notification.builder()
                        .eventId(event.getEventId())
                        .source(event.getSource())
                        .eventType(event.getEventType())
                        .recipientId(event.getRecipient().getUserId())
                        .recipientContact(event.getRecipient().getContactByChannel(channel))
                        .channel(channel)
                        .payload(event.getPayload())
                        .build();

                notificationService.send(notification);

            } catch (Exception exception) {

                log.error(
                        "Invalid data: channel={}, eventId={}, error={}",
                        channel,
                        event.getEventId(),
                        exception.getMessage());

                throw new AmqpRejectAndDontRequeueException(exception); // → DLQ -> retry manually
            }
        }
    }
}
