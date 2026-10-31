package com.project.notificationservice.unit.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.project.notificationservice.consumer.ExternalNotificationConsumer;
import com.project.notificationservice.dto.NotificationEvent;
import com.project.notificationservice.dto.Recipient;
import com.project.notificationservice.entity.Notification;
import com.project.notificationservice.enums.Channel;
import com.project.notificationservice.enums.EventType;
import com.project.notificationservice.enums.ServiceSource;
import com.project.notificationservice.exception.ErrorCode;
import com.project.notificationservice.exception.RateLimitingException;
import com.project.notificationservice.properties.RetryProperties;
import com.project.notificationservice.service.NotificationPreferenceService;
import com.project.notificationservice.service.NotificationProcessorService;
import com.project.notificationservice.service.RateLimitingService;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalNotificationConsumer - Unit Tests")
public class ExternalNotificationConsumerTest {

    @Mock
    private NotificationProcessorService notificationProcessorService;

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private RetryProperties retryProperties;

    @InjectMocks
    private ExternalNotificationConsumer consumer;

    private NotificationEvent event;

    @BeforeEach
    void setUp() {
        Recipient recipient = Recipient.builder()
                .userId("user-123")
                .email("user@example.com")
                .phone("+84901234567")
                .deviceToken("device-token-abc")
                .build();

        event = NotificationEvent.builder()
                .eventId("event-001")
                .eventType(EventType.ORDER_CONFIRMED)
                .source(ServiceSource.ECOMMERCE_SERVICE)
                .recipient(recipient)
                .channels(Set.of(Channel.EMAIL, Channel.SMS))
                .payload(Map.of("orderId", "ORD-123", "amount", 500000))
                .build();

        lenient().when(retryProperties.getMaxRetries()).thenReturn(3);
    }

    // =====================================================
    // 1. Happy path - tất cả channel được bật
    // =====================================================
    @Test
    @DisplayName("1. should process all enabled channels successfully")
    void should_processAllEnabledChannels() {
        // given
        when(preferenceService.isChannelEnabled("user-123", Channel.EMAIL)).thenReturn(true);
        when(preferenceService.isChannelEnabled("user-123", Channel.SMS)).thenReturn(true);

        // when
        consumer.consume(event);

        // then
        verify(rateLimitingService, times(2))
                .rateLimiting(
                        eq("user-123"),
                        any(Channel.class),
                        eq(EventType.ORDER_CONFIRMED),
                        eq(ServiceSource.ECOMMERCE_SERVICE));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationProcessorService, times(2)).sendExternal(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(Notification::getChannel)
                .containsExactlyInAnyOrder(Channel.EMAIL, Channel.SMS);

        // kiểm tra build notification đúng
        Notification emailNoti = captor.getAllValues().stream()
                .filter(n -> n.getChannel() == Channel.EMAIL)
                .findFirst()
                .orElseThrow();

        assertThat(emailNoti.getEventId()).isEqualTo("event-001");
        assertThat(emailNoti.getRecipientId()).isEqualTo("user-123");
        assertThat(emailNoti.getRecipientContact()).isEqualTo("user@example.com");
        assertThat(emailNoti.getMaxRetries()).isEqualTo(3);
        assertThat(emailNoti.getSource()).isEqualTo(ServiceSource.ECOMMERCE_SERVICE);
        assertThat(emailNoti.getEventType()).isEqualTo(EventType.ORDER_CONFIRMED);
    }

    // =====================================================
    // 2. Skip channel bị tắt preference
    // =====================================================
    @Test
    @DisplayName("2. should skip disabled channel and continue with other channels")
    void should_skipDisabledChannel() {
        // given
        when(preferenceService.isChannelEnabled("user-123", Channel.EMAIL)).thenReturn(false); // tắt
        when(preferenceService.isChannelEnabled("user-123", Channel.SMS)).thenReturn(true);

        // when
        consumer.consume(event);

        // then
        // EMAIL bị skip, không gọi rateLimit + sendExternal
        verify(rateLimitingService, never()).rateLimiting(eq("user-123"), eq(Channel.EMAIL), any(), any());

        // SMS vẫn được xử lý
        verify(rateLimitingService, times(1)).rateLimiting(eq("user-123"), eq(Channel.SMS), any(), any());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationProcessorService, times(1)).sendExternal(captor.capture());

        assertThat(captor.getValue().getChannel()).isEqualTo(Channel.SMS);
        assertThat(captor.getValue().getRecipientContact()).isEqualTo("+84901234567");
    }

    // =====================================================
    // 3. Rate limit 1 channel, skip channel đó, tiếp tục channel khác
    // =====================================================
    @Test
    @DisplayName("3. should skip channel when rate limited and continue with other channels")
    void should_skipRateLimitedChannel_and_continue() {
        // given
        when(preferenceService.isChannelEnabled(anyString(), any(Channel.class)))
                .thenReturn(true);

        // EMAIL bị rate limit
        doThrow(new RateLimitingException(ErrorCode.MESSAGE_LIMIT_EXCEEDED))
                .when(rateLimitingService)
                .rateLimiting(eq("user-123"), eq(Channel.EMAIL), any(), any());

        // SMS bình thường
        doNothing().when(rateLimitingService).rateLimiting(eq("user-123"), eq(Channel.SMS), any(), any());

        // when
        consumer.consume(event);

        // then - không ném exception ra ngoài
        verify(notificationProcessorService, times(1)).sendExternal(any(Notification.class));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationProcessorService).sendExternal(captor.capture());
        assertThat(captor.getValue().getChannel()).isEqualTo(Channel.SMS);
    }

    // =====================================================
    // 4. Exception khác, reject message (vào DLQ)
    // =====================================================
    @Test
    @DisplayName("4. should reject message to DLQ when unexpected exception occurs")
    void should_rejectMessage_when_unexpectedException() {
        // given
        when(preferenceService.isChannelEnabled(anyString(), any(Channel.class)))
                .thenReturn(true);

        doNothing().when(rateLimitingService).rateLimiting(any(), any(), any(), any());

        // Giả lập lỗi khi send
        doThrow(new RuntimeException("Database connection failed"))
                .when(notificationProcessorService)
                .sendExternal(any(Notification.class));

        // when + then
        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("Database connection failed");
    }

    // =====================================================
    // 5. Tất cả channel đều bị tắt
    // =====================================================
    @Test
    @DisplayName("5. should do nothing when all channels are disabled")
    void should_doNothing_when_allChannelsDisabled() {
        // given
        when(preferenceService.isChannelEnabled(anyString(), any(Channel.class)))
                .thenReturn(false);

        // when
        consumer.consume(event);

        // then
        verify(rateLimitingService, never()).rateLimiting(any(), any(), any(), any());
        verify(notificationProcessorService, never()).sendExternal(any());
    }

    // =====================================================
    // 6. Kiểm tra mapping contact theo channel
    // =====================================================
    @Nested
    @DisplayName("6. Contact mapping by channel")
    class ContactMappingTests {

        @Test
        @DisplayName("should map correct contact for each channel")
        void should_mapCorrectContact() {
            // given
            event = event.toBuilder()
                    .channels(Set.of(Channel.EMAIL, Channel.SMS, Channel.PUSH, Channel.IN_APP))
                    .build();

            when(preferenceService.isChannelEnabled(anyString(), any(Channel.class)))
                    .thenReturn(true);

            // when
            consumer.consume(event);

            // then
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationProcessorService, times(4)).sendExternal(captor.capture());

            Map<Channel, String> contactMap = captor.getAllValues().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Notification::getChannel, Notification::getRecipientContact));

            assertThat(contactMap)
                    .containsEntry(Channel.EMAIL, "user@example.com")
                    .containsEntry(Channel.SMS, "+84901234567")
                    .containsEntry(Channel.PUSH, "device-token-abc")
                    .containsEntry(Channel.IN_APP, "user-123");
        }
    }

    // =====================================================
    // 7. Chỉ có 1 channel
    // =====================================================
    @Test
    @DisplayName("7. should process single channel event correctly")
    void should_processSingleChannel() {
        // given
        event = event.toBuilder().channels(Set.of(Channel.EMAIL)).build();

        when(preferenceService.isChannelEnabled("user-123", Channel.EMAIL)).thenReturn(true);

        // when
        consumer.consume(event);

        // then
        verify(notificationProcessorService, times(1)).sendExternal(any(Notification.class));
        verify(rateLimitingService, times(1)).rateLimiting(eq("user-123"), eq(Channel.EMAIL), any(), any());
    }
}
