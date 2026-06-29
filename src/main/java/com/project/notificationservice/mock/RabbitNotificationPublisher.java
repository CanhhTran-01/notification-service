package com.project.notificationservice.mock;

import com.project.notificationservice.config.RabbitMQConfig;
import com.project.notificationservice.dto.NotificationEvent;
import com.project.notificationservice.exception.BaseException;
import com.project.notificationservice.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitNotificationPublisher {

    private final AmqpTemplate amqpTemplate;

    public void publish(NotificationEvent event) {
        try {
            amqpTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE, RabbitMQConfig.NOTIFICATION_ROUTING_KEY, event);
            log.info(
                    "Published notification event: eventId={}, eventType={}", event.getEventId(), event.getEventType());

        } catch (AmqpException e) {
            log.error("Failed to publish event: eventId={}, error={}", event.getEventId(), e.getMessage());
            throw new BaseException(ErrorCode.MESSAGE_BROKER_ERROR);
        }
    }
}
