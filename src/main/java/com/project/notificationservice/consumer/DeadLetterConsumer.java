package com.project.notificationservice.consumer;

import com.project.notificationservice.config.RabbitMQConfig;
import com.project.notificationservice.service.DeadLetterService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeadLetterConsumer {

    private final DeadLetterService deadLetterService;

    @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE)
    public void receive(Message message) {

        // message is Raw RabbitMQ message, including: byte[] body + messageProperties

        // get payload from message body
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        // get errorReason + queueName from message properties
        Map<String, Object> headers = message.getMessageProperties().getHeaders();

        @SuppressWarnings("unchecked") // or Check instanceof , depending on scope of project
        List<Map<String, Object>> xDeath = (List<Map<String, Object>>) headers.get("x-death");

        String queueName = "Unknown";
        String errorReason = "Unknown";
        int retryCount = 0;
        if (xDeath != null && !xDeath.isEmpty()) {
            errorReason = xDeath.getFirst().getOrDefault("reason", "Unknown").toString();
            queueName = xDeath.getFirst().get("queue").toString();
            retryCount =
                    Integer.parseInt(xDeath.getFirst().getOrDefault("count", 0).toString());
        }

        deadLetterService.process(payload, queueName, errorReason, retryCount);
    }
}
