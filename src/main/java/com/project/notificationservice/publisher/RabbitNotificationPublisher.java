package com.project.notificationservice.publisher;

import com.project.notificationservice.config.RabbitMQConfig;
import com.project.notificationservice.dto.NotificationEvent;
import com.project.notificationservice.enums.Channel;
import com.project.notificationservice.exception.BaseException;
import com.project.notificationservice.exception.ErrorCode;
import java.util.Set;
import java.util.stream.Collectors;
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

    public void dispatch(NotificationEvent event) {

        if (event.getChannels() == null || event.getChannels().isEmpty()) {
            log.warn("Skipping notification dispatch because channels list is empty. eventId={}", event.getEventId());
            return;
        }

        Set<Channel> externalChannels = event.getChannels().stream()
                .filter(channel -> channel != Channel.IN_APP)
                .collect(Collectors.toSet());

        // EMAIL-SMS-PUSH
        if (!externalChannels.isEmpty()) {
            publish(event.toBuilder().channels(externalChannels).build());
        }

        if (event.getChannels().contains(Channel.IN_APP)) {
            publishInApp(event.toBuilder().channels(Set.of(Channel.IN_APP)).build());
        }
    }

    public void publish(NotificationEvent event) {
        // EMAIL-SMS-PUSH
        send(event, RabbitMQConfig.EXTERNAL_NOTIFICATION_EXCHANGE, RabbitMQConfig.EXTERNAL_NOTIFICATION_ROUTING_KEY);
    }

    public void publishInApp(NotificationEvent event) {
        // IN_APP
        send(event, RabbitMQConfig.INAPP_EXCHANGE, RabbitMQConfig.INAPP_ROUTING_KEY);
    }

    private void send(NotificationEvent event, String exchange, String routingKey) {
        try {
            amqpTemplate.convertAndSend(exchange, routingKey, event);
            log.info(
                    "Published event: eventId={}, eventType={}, exchange={}",
                    event.getEventId(),
                    event.getEventType(),
                    exchange);

        } catch (AmqpException e) {
            log.error("Failed to publish event: eventId={}, error={}", event.getEventId(), e.getMessage());
            throw new BaseException(ErrorCode.MESSAGE_BROKER_ERROR);
        }
    }
}
