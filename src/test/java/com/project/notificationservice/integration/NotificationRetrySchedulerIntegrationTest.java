package com.project.notificationservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.notificationservice.entity.Notification;
import com.project.notificationservice.enums.Channel;
import com.project.notificationservice.enums.EventType;
import com.project.notificationservice.enums.NotificationStatus;
import com.project.notificationservice.enums.ServiceSource;
import com.project.notificationservice.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("NotificationRetryScheduler - Query Integration Test")
public class NotificationRetrySchedulerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("1. should find only PENDING notifications ready for retry")
    void should_findPendingNotificationsReadyForRetry() {
        LocalDateTime now = LocalDateTime.now();

        Notification ready = build(NotificationStatus.PENDING, 1, now.minusMinutes(1), "ready");
        Notification notReady = build(NotificationStatus.PENDING, 1, now.plusMinutes(10), "not-ready");
        Notification exceeded = build(NotificationStatus.PENDING, 3, now.minusMinutes(1), "exceeded");
        Notification sent = build(NotificationStatus.SENT, 0, now.minusMinutes(1), "sent");

        notificationRepository.saveAllAndFlush(List.of(ready, notReady, exceeded, sent));

        List<Notification> result = notificationRepository.findByStatusAndRetryCountLessThanAndNextRetryTimeBefore(
                NotificationStatus.PENDING, 3, LocalDateTime.now());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getEventId()).isEqualTo("ready");
        assertThat(result.getFirst().getRetryCount()).isLessThan(3);
        assertThat(result.getFirst().getNextRetryTime()).isBefore(LocalDateTime.now());
    }

    @Test
    @DisplayName("2. should return empty when no notification is ready")
    void should_returnEmpty_when_noneReady() {
        notificationRepository.saveAndFlush(
                build(NotificationStatus.PENDING, 1, LocalDateTime.now().plusHours(1), "future"));

        List<Notification> result = notificationRepository.findByStatusAndRetryCountLessThanAndNextRetryTimeBefore(
                NotificationStatus.PENDING, 3, LocalDateTime.now());

        assertThat(result).isEmpty();
    }

    private Notification build(NotificationStatus status, int retryCount, LocalDateTime nextRetryTime, String eventId) {
        return Notification.builder()
                .eventId(eventId)
                .source(ServiceSource.ECOMMERCE_SERVICE)
                .recipientId("user-123")
                .recipientContact("user@example.com")
                .channel(Channel.EMAIL)
                .eventType(EventType.ORDER_CONFIRMED)
                .status(status)
                .maxRetries(3)
                .retryCount(retryCount)
                .nextRetryTime(nextRetryTime)
                .build();
    }
}
