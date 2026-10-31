package com.project.notificationservice.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.project.notificationservice.dto.NotificationResponse;
import com.project.notificationservice.entity.Notification;
import com.project.notificationservice.enums.Channel;
import com.project.notificationservice.enums.EventType;
import com.project.notificationservice.enums.NotificationStatus;
import com.project.notificationservice.enums.ServiceSource;
import com.project.notificationservice.exception.BaseException;
import com.project.notificationservice.exception.ErrorCode;
import com.project.notificationservice.repository.NotificationRepository;
import com.project.notificationservice.service.impl.UserNotificationServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserNotificationServiceImpl - Unit Tests")
public class UserNotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private UserNotificationServiceImpl userNotificationService;

    private static final String RECIPIENT_ID = "user-123";
    private static final UUID NOTIFICATION_ID = UUID.randomUUID();

    private Notification notification1;
    private Notification notification2;

    @BeforeEach
    void setUp() {
        notification1 = Notification.builder()
                .id(NOTIFICATION_ID)
                .recipientId(RECIPIENT_ID)
                .recipientContact(RECIPIENT_ID)
                .channel(Channel.IN_APP)
                .eventType(EventType.ORDER_CONFIRMED)
                .source(ServiceSource.ECOMMERCE_SERVICE)
                .eventId("event-001")
                .status(NotificationStatus.SENT)
                .isRead(false)
                .payload(Map.of("orderId", "ORD-123"))
                .maxRetries(3)
                .sentAt(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        notification2 = Notification.builder()
                .id(UUID.randomUUID())
                .recipientId(RECIPIENT_ID)
                .recipientContact(RECIPIENT_ID)
                .channel(Channel.IN_APP)
                .eventType(EventType.WELCOME)
                .source(ServiceSource.IDENTITY_SERVICE)
                .eventId("event-002")
                .status(NotificationStatus.SENT)
                .isRead(true)
                .payload(Map.of("message", "Welcome!"))
                .maxRetries(3)
                .sentAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    // =====================================================
    // 1. getHistory()
    // =====================================================
    @Nested
    @DisplayName("1. getHistory()")
    class GetHistoryTests {

        @Test
        @DisplayName("should return paginated IN_APP notification history")
        void should_returnPaginatedHistory() {
            // given
            Pageable pageable = PageRequest.of(0, 5);
            Page<Notification> page = new PageImpl<>(List.of(notification1, notification2), pageable, 2);

            when(notificationRepository.findByRecipientIdAndChannelOrderByCreatedAtDesc(
                            eq(RECIPIENT_ID), eq(Channel.IN_APP), eq(pageable)))
                    .thenReturn(page);

            // when
            Page<NotificationResponse> result = userNotificationService.getHistory(RECIPIENT_ID, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);

            NotificationResponse first = result.getContent().getFirst();
            assertThat(first.getId()).isEqualTo(NOTIFICATION_ID);
            assertThat(first.getEventType()).isEqualTo(EventType.ORDER_CONFIRMED);
            assertThat(first.isRead()).isFalse();
            assertThat(first.getPayload()).containsEntry("orderId", "ORD-123");
            assertThat(first.getSentAt()).isNotNull();
            assertThat(first.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("2. should return empty page when no history")
        void should_returnEmptyPage_when_noHistory() {
            // given
            Pageable pageable = PageRequest.of(0, 5);
            when(notificationRepository.findByRecipientIdAndChannelOrderByCreatedAtDesc(
                            eq(RECIPIENT_ID), eq(Channel.IN_APP), eq(pageable)))
                    .thenReturn(Page.empty(pageable));

            // when
            Page<NotificationResponse> result = userNotificationService.getHistory(RECIPIENT_ID, pageable);

            // then
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // =====================================================
    // 2. updateRead()
    // =====================================================
    @Nested
    @DisplayName("3. updateRead()")
    class UpdateReadTests {

        @Test
        @DisplayName("should mark notification as read successfully")
        void should_markAsRead_successfully() {
            // given
            when(notificationRepository.findByIdAndRecipientId(NOTIFICATION_ID, RECIPIENT_ID))
                    .thenReturn(Optional.of(notification1));

            // when
            userNotificationService.updateRead(NOTIFICATION_ID.toString(), RECIPIENT_ID);

            // then
            assertThat(notification1.isRead()).isTrue();
            assertThat(notification1.getReadAt()).isNotNull();
        }

        @Test
        @DisplayName("4. should throw NOTIFICATION_NOT_FOUND when notification does not exist")
        void should_throw_when_notificationNotFound() {
            // given
            when(notificationRepository.findByIdAndRecipientId(NOTIFICATION_ID, RECIPIENT_ID))
                    .thenReturn(Optional.empty());

            // when + then
            assertThatThrownBy(() -> userNotificationService.updateRead(NOTIFICATION_ID.toString(), RECIPIENT_ID))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> {
                        BaseException be = (BaseException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("5. should throw NOTIFICATION_NOT_FOUND when recipientId does not match (IDOR protection)")
        void should_throw_when_recipientIdNotMatch() {
            // given - tìm theo id + recipientId khác → empty
            when(notificationRepository.findByIdAndRecipientId(NOTIFICATION_ID, "other-user"))
                    .thenReturn(Optional.empty());

            // when + then
            assertThatThrownBy(() -> userNotificationService.updateRead(NOTIFICATION_ID.toString(), "other-user"))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> {
                        BaseException be = (BaseException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
                    });
        }
    }

    // =====================================================
    // 3. countNotReadNotification()
    // =====================================================
    @Nested
    @DisplayName("6. countNotReadNotification()")
    class CountNotReadTests {

        @Test
        @DisplayName("should return correct unread count")
        void should_returnCorrectUnreadCount() {
            // given
            when(notificationRepository.countByRecipientIdAndChannelAndIsReadFalse(RECIPIENT_ID, Channel.IN_APP))
                    .thenReturn(5L);

            // when
            Long count = userNotificationService.countNotReadNotification(RECIPIENT_ID);

            // then
            assertThat(count).isEqualTo(5L);
        }

        @Test
        @DisplayName("7. should return 0 when no unread notification")
        void should_returnZero_when_noUnread() {
            // given
            when(notificationRepository.countByRecipientIdAndChannelAndIsReadFalse(RECIPIENT_ID, Channel.IN_APP))
                    .thenReturn(0L);

            // when
            Long count = userNotificationService.countNotReadNotification(RECIPIENT_ID);

            // then
            assertThat(count).isZero();
        }
    }
}
