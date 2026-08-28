package com.project.notificationservice.consumer;

import com.project.notificationservice.config.RabbitMQConfig;
import com.project.notificationservice.dto.NotificationEvent;
import com.project.notificationservice.entity.Notification;
import com.project.notificationservice.exception.RateLimitingException;
import com.project.notificationservice.properties.RetryProperties;
import com.project.notificationservice.service.NotificationPreferenceService;
import com.project.notificationservice.service.NotificationProcessorService;
import com.project.notificationservice.service.RateLimitingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExternalNotificationConsumer {

    private final NotificationProcessorService notificationProcessorService;
    private final RateLimitingService rateLimitingService;
    private final NotificationPreferenceService preferenceService;
    private final RetryProperties retryProperties;

    @RabbitListener(queues = RabbitMQConfig.EXTERNAL_NOTIFICATION_QUEUE)
    public void consume(NotificationEvent event) {

        for (var channel : event.getChannels()) {
            try {
                // Double-check preference trước khi xử lý, không tin tưởng external service
                boolean isEnabled =
                        preferenceService.isChannelEnabled(event.getRecipient().getUserId(), channel);

                if (!isEnabled) {
                    log.info(
                            "Channel {} disabled for recipientId={}, skipping",
                            channel,
                            event.getRecipient().getUserId());
                    continue; // skip channel này, tiếp tục channel khác
                }

                // Rate-limiting
                rateLimitingService.rateLimiting(
                        event.getRecipient().getUserId(), channel, event.getEventType(), event.getSource());

                // Build notification
                Notification notification = Notification.builder()
                        .eventId(event.getEventId())
                        .source(event.getSource())
                        .eventType(event.getEventType())
                        .recipientId(event.getRecipient().getUserId())
                        .recipientContact(event.getRecipient().getContactByChannel(channel))
                        .channel(channel)
                        .payload(event.getPayload())
                        .maxRetries(retryProperties.getMaxRetries())
                        .build();

                // send
                notificationProcessorService.sendExternal(notification);

            } catch (RateLimitingException exception) {

                // Không trace bất kì message nào rơi vào đây, chỉ log
                log.warn(
                        "Rate limit triggered: recipientId={}, channel={}, eventId={}, cause={}",
                        event.getRecipient().getUserId(),
                        channel,
                        event.getEventId(),
                        exception.getMessage());

            } catch (Exception exception) {

                // 1 channel lỗi -> dừng ngay việc gửi, ném tin sang DLQ để retry
                // idempotecy(eventId + channel) giúp không gửi lại tin ở channel đã gửi rồi khi replay/retry
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
