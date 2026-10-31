package com.project.notificationservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.notificationservice.entity.Notification;
import com.project.notificationservice.enums.Channel;
import com.project.notificationservice.enums.EventType;
import com.project.notificationservice.enums.NotificationStatus;
import com.project.notificationservice.enums.ServiceSource;
import com.project.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@DisplayName("NotificationRepository - Unique constraint Integration Test")
public class NotificationRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("1. should save notification successfully when eventId + channel is unique")
    void should_save_when_unique() {
        Notification notification = build(Channel.EMAIL);

        Notification saved = notificationRepository.saveAndFlush(notification);

        assertThat(saved.getId()).isNotNull();
        assertThat(notificationRepository.existsByEventIdAndChannel("event-001", Channel.EMAIL))
                .isTrue();
    }

    @Test
    @DisplayName("2. should throw when duplicate eventId + channel")
    void should_throw_when_duplicateEventIdAndChannel() {
        notificationRepository.saveAndFlush(build(Channel.EMAIL));

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(build(Channel.EMAIL)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("3. should allow same eventId with different channel")
    void should_allow_sameEventId_differentChannel() {

        long before = notificationRepository.count();

        notificationRepository.saveAndFlush(build(Channel.EMAIL));
        notificationRepository.saveAndFlush(build(Channel.SMS));

        assertThat(notificationRepository.count()).isEqualTo(before + 2);
    }

    private Notification build(Channel channel) {
        return Notification.builder()
                .eventId("event-001")
                .source(ServiceSource.ECOMMERCE_SERVICE)
                .recipientId("user-123")
                .recipientContact("user@example.com")
                .channel(channel)
                .eventType(EventType.ORDER_CONFIRMED)
                .status(NotificationStatus.PENDING)
                .maxRetries(3)
                .retryCount(0)
                .build();
    }
}
