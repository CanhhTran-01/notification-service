package com.project.notificationservice.consumer;

import com.project.notificationservice.config.RabbitMQConfig;
import com.project.notificationservice.config.properties.RetryProperties;
import com.project.notificationservice.domain.entity.Notification;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.dto.NotificationEvent;
import com.project.notificationservice.exception.RateLimitingException;
import com.project.notificationservice.service.NotificationPreferenceService;
import com.project.notificationservice.service.NotificationProcessorService;
import com.project.notificationservice.service.RateLimitingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class InAppNotificationConsumer {

    private final NotificationProcessorService notificationProcessorService;
    private final NotificationPreferenceService preferenceService;
    private final RateLimitingService rateLimitingService;
    private final RetryProperties retryProperties;

    @RabbitListener(queues = RabbitMQConfig.INAPP_QUEUE, containerFactory = "inAppListenerContainerFactory")
    public void consume(NotificationEvent event) {

        try {
            // Check preference — user có tắt IN_APP không?
            boolean isEnabled =
                    preferenceService.isChannelEnabled(event.getRecipient().getUserId(), Channel.IN_APP);

            if (!isEnabled) {
                log.info(
                        "Channel {} disabled for recipientId={}, skipping",
                        Channel.IN_APP,
                        event.getRecipient().getUserId());
                return;
            }

            // Rate-limiting
            rateLimitingService.rateLimiting(
                    event.getRecipient().getUserId(), Channel.IN_APP, event.getEventType(), event.getSource());

            // Build notification
            Notification notification = Notification.builder()
                    .eventId(event.getEventId())
                    .source(event.getSource())
                    .eventType(event.getEventType())
                    .recipientId(event.getRecipient().getUserId())
                    .recipientContact(event.getRecipient().getContactByChannel(Channel.IN_APP))
                    .channel(Channel.IN_APP)
                    .payload(event.getPayload())
                    .maxRetries(retryProperties.getMaxRetries())
                    .build();

            // send
            notificationProcessorService.sendInApp(notification);

        } catch (RateLimitingException exception) {

            // Không trace bất kì message nào rơi vào đây, chỉ log
            log.warn(
                    "Rate limit triggered: recipientId={}, channel={}, eventId={}, cause={}",
                    event.getRecipient().getUserId(),
                    Channel.IN_APP,
                    event.getEventId(),
                    exception.getMessage());
        }
    }
}
