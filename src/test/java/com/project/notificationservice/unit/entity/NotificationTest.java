package com.project.notificationservice.unit.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.project.notificationservice.entity.Notification;
import com.project.notificationservice.enums.Channel;
import com.project.notificationservice.enums.EventType;
import com.project.notificationservice.enums.NotificationStatus;
import com.project.notificationservice.enums.ServiceSource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Notification Entity - Domain Logic")
public class NotificationTest {

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = Notification.builder()
                .source(ServiceSource.CHAT_SERVICE)
                .recipientId("user-123")
                .recipientContact("user@example.com")
                .channel(Channel.EMAIL)
                .eventType(EventType.WELCOME)
                .eventId("event-001")
                .maxRetries(3)
                .retryCount(0)
                .status(NotificationStatus.PENDING)
                .isRead(false)
                .build();
    }

    // =====================================================
    // 1. markAsSent()
    // =====================================================
    @Test
    @DisplayName("1. markAsSent() - should set status to SENT and set sentAt")
    void should_markAsSent_successfully() {
        // when
        notification.markAsSent();

        // then
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT); // đúng SENT chưa ?
        assertThat(notification.getSentAt()).isNotNull(); // có dữ liệu sentAt chưa ?
        assertThat(notification.getSentAt()).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS));
    }

    // =====================================================
    // 2. markAsFailed()
    // =====================================================
    @Test
    @DisplayName("2. markAsFailed() - should set status to FAILED and record errorMessage")
    void should_markAsFailed_withErrorMessage() {
        // given
        String errorMessage = "This is error";

        // when
        notification.markAsFailed(errorMessage);

        // then
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getErrorMessage()).isEqualTo(errorMessage);
    }

    // =====================================================
    // 3 & 4. incrementRetryAndCalculateNextTime()
    // =====================================================
    @Nested
    @DisplayName("3. incrementRetryAndCalculateNextTime()")
    class IncrementRetryTests {

        @Test
        @DisplayName(
                "should increase retryCount, set status PENDING and calculate nextRetryTime when still has retries left")
        void should_incrementRetry_and_calculateNextTime_when_hasRetriesLeft() {
            // given
            long baseDelaySeconds = 60;
            notification = Notification.builder()
                    .maxRetries(3)
                    .retryCount(0)
                    .status(NotificationStatus.PROCESSING)
                    .build();

            LocalDateTime before = LocalDateTime.now();

            // when
            notification.incrementRetryAndCalculateNextTime(baseDelaySeconds);

            // then
            assertThat(notification.getRetryCount()).isEqualTo(1);
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(notification.getNextRetryTime()).isNotNull();

            // delay = 60 * 2^(1-1) = 60 seconds
            LocalDateTime expected = before.plusSeconds(60);
            assertThat(notification.getNextRetryTime()).isCloseTo(expected, within(2, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("4. should calculate correct exponential backoff on second retry")
        void should_calculate_exponentialBackoff_onSecondRetry() {
            // given
            long baseDelaySeconds = 60;
            notification = Notification.builder()
                    .maxRetries(5)
                    .retryCount(1) // đã retry 1 lần
                    .status(NotificationStatus.PROCESSING)
                    .build();

            LocalDateTime before = LocalDateTime.now();

            // when
            notification.incrementRetryAndCalculateNextTime(baseDelaySeconds);

            // then
            assertThat(notification.getRetryCount()).isEqualTo(2);
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);

            // delay = 60 * 2^(2-1) = 120 seconds
            LocalDateTime expected = before.plusSeconds(120);
            assertThat(notification.getNextRetryTime()).isCloseTo(expected, within(2, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("5. should mark as FAILED and set nextRetryTime = null when reaching maxRetries")
        void should_markAsFailed_when_retryCountReachesMaxRetries() {
            // given
            long baseDelaySeconds = 60;
            notification = Notification.builder()
                    .maxRetries(3)
                    .retryCount(2) // lần retry tiếp theo sẽ = 3 → đạt max
                    .status(NotificationStatus.PROCESSING)
                    .build();

            // when
            notification.incrementRetryAndCalculateNextTime(baseDelaySeconds);

            // then
            assertThat(notification.getRetryCount()).isEqualTo(3);
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(notification.getErrorMessage()).isEqualTo("Đã đạt mức retry tối đa");
            assertThat(notification.getNextRetryTime()).isNull();
        }

        @Test
        @DisplayName("6. should mark as FAILED when retryCount already equals maxRetries")
        void should_markAsFailed_when_retryCountAlreadyEqualsMaxRetries() {
            // given
            notification = Notification.builder()
                    .maxRetries(3)
                    .retryCount(3)
                    .status(NotificationStatus.PROCESSING)
                    .build();

            // when
            notification.incrementRetryAndCalculateNextTime(60);

            // then
            assertThat(notification.getRetryCount()).isEqualTo(4);
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
            assertThat(notification.getNextRetryTime()).isNull();
        }
    }

    // =====================================================
    // 5. markAsRead()
    // =====================================================
    @Test
    @DisplayName("7. markAsRead() - should set isRead = true and set readAt")
    void should_markAsRead_successfully() {
        // when
        notification.markAsRead();

        // then
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        assertThat(notification.getReadAt()).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS));
    }

    // =====================================================
    // 6. resetForRetry()
    // =====================================================
    @Test
    @DisplayName("8. resetForRetry() - should reset status, retryCount, errorMessage and set nextRetryTime to now")
    void should_resetForRetry_successfully() {
        // given
        notification = Notification.builder()
                .status(NotificationStatus.FAILED)
                .retryCount(2)
                .errorMessage("Previous error")
                .nextRetryTime(LocalDateTime.now().plusHours(1))
                .maxRetries(3)
                .build();

        LocalDateTime before = LocalDateTime.now();

        // when
        notification.resetForRetry();

        // then
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getRetryCount()).isEqualTo(0);
        assertThat(notification.getErrorMessage()).isNull();
        assertThat(notification.getNextRetryTime()).isNotNull();
        assertThat(notification.getNextRetryTime()).isCloseTo(before, within(2, ChronoUnit.SECONDS));
    }

    // =====================================================
    // Các method liên quan status
    // =====================================================
    @Test
    @DisplayName("9. markAsProcessing() - should set status to PROCESSING")
    void should_markAsProcessing() {
        notification.markAsProcessing();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PROCESSING);
    }

    @Test
    @DisplayName("10. markAsRetrying() - should set status to RETRYING")
    void should_markAsRetrying() {
        notification.markAsRetrying();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.RETRYING);
    }

    @Test
    @DisplayName("11. cancelling() - should set status to FAILED and record errorMessage")
    void should_cancelling_withErrorMessage() {
        notification.cancelling("User cancelled");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getErrorMessage()).isEqualTo("User cancelled");
    }
}
