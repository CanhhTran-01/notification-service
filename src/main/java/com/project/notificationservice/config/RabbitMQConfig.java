package com.project.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.routing.key";

    public static final String INAPP_QUEUE = "notification.inapp.queue";
    public static final String INAPP_EXCHANGE = "notification.inapp.exchange";
    public static final String INAPP_ROUTING_KEY = "notification.inapp.routing.key";

    public static final String DLQ_QUEUE = "notification.dlq.queue";
    public static final String DLQ_EXCHANGE = "notification.dlq.exchange";
    public static final String DLQ_ROUTING_KEY = "notification.dlq.routing.key";

    @Value("${app.rabbitmq.inapp.concurrent-consumers}")
    private int inAppConcurrentConsumers;

    @Value("${app.rabbitmq.inapp.max-concurrent-consumers}")
    private int inAppMaxConcurrentConsumers;

    /*
    ======================================================
    =========== Notification queue =======================
    ======================================================
    */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Binding notificationBinding(
            @Qualifier("notificationQueue") Queue notificationQueue,
            @Qualifier("notificationExchange") DirectExchange notificationExchange) {

        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    /*
    ======================================================
    ============== IN_APP queue ==========================
    ======================================================
    */
    @Bean
    public Queue inAppQueue() {
        return QueueBuilder.durable(INAPP_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY) // chung DLQ
                .build();
    }

    @Bean
    public DirectExchange inAppExchange() {
        return new DirectExchange(INAPP_EXCHANGE);
    }

    @Bean
    public Binding inAppBinding(
            @Qualifier("inAppQueue") Queue inAppQueue, @Qualifier("inAppExchange") DirectExchange inAppExchange) {

        return BindingBuilder.bind(inAppQueue).to(inAppExchange).with(INAPP_ROUTING_KEY);
    }

    /*
    ======================================================
    ========= Dead letter queue ==========================
    ======================================================
    */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLQ_EXCHANGE);
    }

    @Bean
    public Binding deadLetterBinding(
            @Qualifier("deadLetterQueue") Queue deadLetterQueue,
            @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {

        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ_ROUTING_KEY);
    }

    // Cấu hình RabbitMQ sử dụng JSON để gửi và nhận message
    // (Giúp dễ đọc data trên RabbitMQ Management UI và giao tiếp tốt với các service khác)
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

    // Concurrency config cho IN_APP listener
    @Bean
    public SimpleRabbitListenerContainerFactory inAppListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(inAppConcurrentConsumers);
        factory.setMaxConcurrentConsumers(inAppMaxConcurrentConsumers);
        return factory;
    }
}
