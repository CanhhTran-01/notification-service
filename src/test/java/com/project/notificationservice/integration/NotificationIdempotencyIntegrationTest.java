package com.project.notificationservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.notificationservice.entity.Notification;
import com.project.notificationservice.enums.Channel;
import com.project.notificationservice.enums.EventType;
import com.project.notificationservice.enums.NotificationStatus;
import com.project.notificationservice.enums.ServiceSource;
import com.project.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Notification Processor - Idempotency Integration Test")
public class NotificationIdempotencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("1. should detect duplicate eventId + channel")
    void should_detect_duplicate() {
        long before = notificationRepository.count();
        notificationRepository.saveAndFlush(build("event-dup-001", Channel.EMAIL));

        boolean exists = notificationRepository.existsByEventIdAndChannel("event-dup-001", Channel.EMAIL);

        assertThat(exists).isTrue();
        assertThat(notificationRepository.count()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("2. should allow same eventId on different channel")
    void should_allow_sameEventId_onDifferentChannel() {

        long before = notificationRepository.count();

        notificationRepository.saveAndFlush(build("event-002", Channel.EMAIL));
        notificationRepository.saveAndFlush(build("event-002", Channel.SMS));

        assertThat(notificationRepository.count()).isEqualTo(before + 2);
        assertThat(notificationRepository.existsByEventIdAndChannel("event-002", Channel.EMAIL))
                .isTrue();
        assertThat(notificationRepository.existsByEventIdAndChannel("event-002", Channel.SMS))
                .isTrue();
    }

    private Notification build(String eventId, Channel channel) {
        return Notification.builder()
                .eventId(eventId)
                .source(ServiceSource.ECOMMERCE_SERVICE)
                .recipientId("user-123")
                .recipientContact("contact")
                .channel(channel)
                .eventType(EventType.ORDER_CONFIRMED)
                .status(NotificationStatus.SENT)
                .maxRetries(3)
                .retryCount(0)
                .build();
    }
}
