package com.project.notificationservice.unit.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.project.notificationservice.entity.RateLimiting;
import com.project.notificationservice.enums.Channel;
import com.project.notificationservice.enums.EventType;
import com.project.notificationservice.enums.ServiceSource;
import com.project.notificationservice.exception.ErrorCode;
import com.project.notificationservice.exception.RateLimitingException;
import com.project.notificationservice.properties.RateLimitProperties;
import com.project.notificationservice.repository.RateLimitingRepository;
import com.project.notificationservice.service.impl.RateLimitingServiceImpl;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingServiceImpl - Unit Tests")
public class RateLimitingServiceImplTest {

    @Mock
    private RateLimitingRepository rateLimitingRepository;

    @Mock
    private RateLimitProperties rateLimitProperties;

    @InjectMocks
    private RateLimitingServiceImpl rateLimitingService;

    private static final String RECIPIENT_ID = "user-123";
    private static final Channel CHANNEL = Channel.EMAIL;
    private static final EventType EVENT_TYPE = EventType.ORDER_CONFIRMED;
    private static final ServiceSource SOURCE = ServiceSource.ECOMMERCE_SERVICE;

    RateLimitProperties.ChannelLimit emailLimit = new RateLimitProperties.ChannelLimit();

    @BeforeEach
    void setUp() {
        emailLimit.setMaxCount(5);
        emailLimit.setWindowMinutes(60);
        emailLimit.setMinIntervalSeconds(60);

        // mặc định luôn có config cho EMAIL
        when(rateLimitProperties.getLimits()).thenReturn(Map.of("email", emailLimit));
    }

    // =====================================================
    // 1. Happy path - chưa có record → tạo mới
    // =====================================================
    @Test
    @DisplayName("1. should allow and create new records when no existing rate limit record")
    void should_allow_and_createNewRecords_when_noExistingRecord() {
        // given
        when(rateLimitingRepository.findByRecipientIdAndChannelAndEventTypeAndSource(
                        anyString(), any(Channel.class), any(EventType.class), any(ServiceSource.class)))
                .thenReturn(Optional.empty());

        when(rateLimitingRepository.saveAndFlush(any(RateLimiting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        rateLimitingService.rateLimiting(RECIPIENT_ID, CHANNEL, EVENT_TYPE, SOURCE);

        // then - gọi 3 lần (3 layer pattern)
        verify(rateLimitingRepository, times(3)).saveAndFlush(any(RateLimiting.class));

        ArgumentCaptor<RateLimiting> captor = ArgumentCaptor.forClass(RateLimiting.class);
        verify(rateLimitingRepository, times(3)).saveAndFlush(captor.capture());

        RateLimiting saved = captor.getAllValues().getFirst();
        assertThat(saved.getRecipientId()).isEqualTo(RECIPIENT_ID);
        assertThat(saved.getChannel()).isEqualTo(CHANNEL);
        assertThat(saved.getSendCount()).isEqualTo(1);
        assertThat(saved.getWindowEnd()).isNotNull();
        assertThat(saved.getLastSendAt()).isNotNull();
    }

    // =====================================================
    // 2. Còn trong window + chưa vượt maxCount → cho phép + tăng sendCount
    // =====================================================
    @Test
    @DisplayName("2. should allow and increase sendCount when within window and under maxCount")
    void should_allow_and_increaseSendCount_when_withinWindowAndUnderMaxCount() {
        // given: build 3 layer
        LocalDateTime now = LocalDateTime.now();
        RateLimiting layer1 = buildRecord(EventType.ORDER_CONFIRMED, ServiceSource.GLOBAL, now);
        RateLimiting layer2 = buildRecord(EventType.GLOBAL, SOURCE, now);
        RateLimiting layer3 = buildRecord(EventType.GLOBAL, ServiceSource.GLOBAL, now);

        // 3 layer đều trả về record hợp lệ
        when(rateLimitingRepository.findByRecipientIdAndChannelAndEventTypeAndSource(
                        RECIPIENT_ID, CHANNEL, EVENT_TYPE, ServiceSource.GLOBAL))
                .thenReturn(Optional.of(layer1));

        when(rateLimitingRepository.findByRecipientIdAndChannelAndEventTypeAndSource(
                        RECIPIENT_ID, CHANNEL, EventType.GLOBAL, SOURCE))
                .thenReturn(Optional.of(layer2));

        when(rateLimitingRepository.findByRecipientIdAndChannelAndEventTypeAndSource(
                        RECIPIENT_ID, CHANNEL, EventType.GLOBAL, ServiceSource.GLOBAL))
                .thenReturn(Optional.of(layer3));

        // when
        rateLimitingService.rateLimiting(RECIPIENT_ID, CHANNEL, EVENT_TYPE, SOURCE);

        // then
        assertThat(layer1.getSendCount()).isEqualTo(3);
        assertThat(layer2.getSendCount()).isEqualTo(3);
        assertThat(layer3.getSendCount()).isEqualTo(3);

        assertThat(layer1.getLastSendAt()).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS));
        verify(rateLimitingRepository, never()).saveAndFlush(any());
    }

    // =====================================================
    // 3. Vượt maxCount trong window → ném MESSAGE_LIMIT_EXCEEDED
    // =====================================================
    @Test
    @DisplayName("3. should throw MESSAGE_LIMIT_EXCEEDED when sendCount reaches maxCount")
    void should_throw_when_maxCountExceeded() {
        // given
        LocalDateTime now = LocalDateTime.now();
        RateLimiting existing = RateLimiting.builder()
                .recipientId(RECIPIENT_ID)
                .channel(CHANNEL)
                .eventType(EVENT_TYPE)
                .source(ServiceSource.GLOBAL)
                .sendCount(5) // = maxCount
                .windowEnd(now.plusMinutes(30))
                .lastSendAt(now.minusSeconds(120))
                .build();

        when(rateLimitingRepository.findByRecipientIdAndChannelAndEventTypeAndSource(
                        eq(RECIPIENT_ID), eq(CHANNEL), eq(EVENT_TYPE), eq(ServiceSource.GLOBAL)))
                .thenReturn(Optional.of(existing));

        // when + then
        assertThatThrownBy(() -> rateLimitingService.rateLimiting(RECIPIENT_ID, CHANNEL, EVENT_TYPE, SOURCE))
                .isInstanceOf(RateLimitingException.class)
                .satisfies(ex -> {
                    RateLimitingException rle = (RateLimitingException) ex;
                    assertThat(rle.getErrorCode()).isEqualTo(ErrorCode.MESSAGE_LIMIT_EXCEEDED);
                });
    }

    // =====================================================
    // 4. Chưa đủ min-interval → ném REQUEST_TOO_FREQUENT
    // =====================================================
    @Test
    @DisplayName("4. should throw REQUEST_TOO_FREQUENT when request is too frequent")
    void should_throw_when_requestTooFrequent() {
        // given
        LocalDateTime now = LocalDateTime.now();
        RateLimiting existing = RateLimiting.builder()
                .recipientId(RECIPIENT_ID)
                .channel(CHANNEL)
                .eventType(EVENT_TYPE)
                .source(ServiceSource.GLOBAL)
                .sendCount(1)
                .windowEnd(now.plusMinutes(30))
                .lastSendAt(now.minusSeconds(10)) // chỉ cách 10s < 60s
                .build();

        when(rateLimitingRepository.findByRecipientIdAndChannelAndEventTypeAndSource(
                        eq(RECIPIENT_ID), eq(CHANNEL), eq(EVENT_TYPE), eq(ServiceSource.GLOBAL)))
                .thenReturn(Optional.of(existing));

        // when + then
        assertThatThrownBy(() -> rateLimitingService.rateLimiting(RECIPIENT_ID, CHANNEL, EVENT_TYPE, SOURCE))
                .isInstanceOf(RateLimitingException.class)
                .satisfies(ex -> {
                    RateLimitingException rle = (RateLimitingException) ex;
                    assertThat(rle.getErrorCode()).isEqualTo(ErrorCode.REQUEST_TOO_FREQUENT);
                });
    }

    // =====================================================
    // 5. Hết window → reset window và cho phép gửi
    // =====================================================
    @Test
    @DisplayName("5. should reset window and allow when window has expired")
    void should_resetWindow_when_windowExpired() {
        // given
        LocalDateTime now = LocalDateTime.now();

        RateLimiting layer1 = buildExpiredRecord(EventType.ORDER_CONFIRMED, ServiceSource.GLOBAL, now);
        RateLimiting layer2 = buildExpiredRecord(EventType.GLOBAL, SOURCE, now);
        RateLimiting layer3 = buildExpiredRecord(EventType.GLOBAL, ServiceSource.GLOBAL, now);

        when(rateLimitingRepository.findByRecipientIdAndChannelAndEventTypeAndSource(
                        RECIPIENT_ID, CHANNEL, EVENT_TYPE, ServiceSource.GLOBAL))
                .thenReturn(Optional.of(layer1));

        when(rateLimitingRepository.findByRecipientIdAndChannelAndEventTypeAndSource(
                        RECIPIENT_ID, CHANNEL, EventType.GLOBAL, SOURCE))
                .thenReturn(Optional.of(layer2));

        when(rateLimitingRepository.findByRecipientIdAndChannelAndEventTypeAndSource(
                        RECIPIENT_ID, CHANNEL, EventType.GLOBAL, ServiceSource.GLOBAL))
                .thenReturn(Optional.of(layer3));

        // when
        rateLimitingService.rateLimiting(RECIPIENT_ID, CHANNEL, EVENT_TYPE, SOURCE);

        // then
        assertThat(layer1.getSendCount()).isEqualTo(1);
        assertThat(layer2.getSendCount()).isEqualTo(1);
        assertThat(layer3.getSendCount()).isEqualTo(1);

        assertThat(layer1.getWindowEnd()).isAfter(now);
        assertThat(layer1.getLastSendAt()).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS));
    }

    // =====================================================
    // 6. Không có config cho channel → bỏ qua (cho phép)
    // =====================================================
    @Test
    @DisplayName("6. should skip rate limiting when no config for channel")
    void should_skip_when_noConfigForChannel() {
        // given
        when(rateLimitProperties.getLimits()).thenReturn(Map.of()); // empty

        // when
        rateLimitingService.rateLimiting(RECIPIENT_ID, CHANNEL, EVENT_TYPE, SOURCE);

        // then
        verify(rateLimitingRepository, never())
                .findByRecipientIdAndChannelAndEventTypeAndSource(any(), any(), any(), any());
        verify(rateLimitingRepository, never()).saveAndFlush(any());
    }

    // =====================================================
    // 7. Xử lý race condition (DataIntegrityViolationException)
    // =====================================================
    @Test
    @DisplayName("7. should handle concurrent insert by reloading record after DataIntegrityViolation")
    void should_handle_concurrentInsert_withDataIntegrityViolation() {
        // given
        LocalDateTime now = LocalDateTime.now();
        RateLimiting existingAfterRace = RateLimiting.builder()
                .recipientId(RECIPIENT_ID)
                .channel(CHANNEL)
                .eventType(EVENT_TYPE)
                .source(ServiceSource.GLOBAL)
                .sendCount(1)
                .windowEnd(now.plusMinutes(60))
                .lastSendAt(now.minusSeconds(120))
                .build();

        // lần đầu không tìm thấy → cố insert → bị violation → load lại
        when(rateLimitingRepository.findByRecipientIdAndChannelAndEventTypeAndSource(
                        eq(RECIPIENT_ID), eq(CHANNEL), eq(EVENT_TYPE), eq(ServiceSource.GLOBAL)))
                .thenReturn(Optional.empty()) // lần 1
                .thenReturn(Optional.of(existingAfterRace)); // lần 2 (sau violation)

        // 2 layer còn lại cũng trả empty rồi save thành công
        when(rateLimitingRepository.findByRecipientIdAndChannelAndEventTypeAndSource(
                        eq(RECIPIENT_ID), eq(CHANNEL), eq(EventType.GLOBAL), any(ServiceSource.class)))
                .thenReturn(Optional.empty());

        when(rateLimitingRepository.saveAndFlush(any(RateLimiting.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate")) // layer 1
                .thenAnswer(inv -> inv.getArgument(0)) // layer 2
                .thenAnswer(inv -> inv.getArgument(0)); // layer 3

        // when
        rateLimitingService.rateLimiting(RECIPIENT_ID, CHANNEL, EVENT_TYPE, SOURCE);

        // then - vẫn chạy thành công, không ném exception
        assertThat(existingAfterRace.getSendCount()).isEqualTo(2);
    }

    // =====================================================
    // Nested: kiểm tra rõ từng layer pattern
    // =====================================================
    @Nested
    @DisplayName("8. 3-layer pattern verification")
    class LayerPatternTests {

        @Test
        @DisplayName("should check 3 patterns: eventType, source, and global")
        void should_check_threeLayers() {
            when(rateLimitingRepository.findByRecipientIdAndChannelAndEventTypeAndSource(
                            anyString(), any(Channel.class), any(EventType.class), any(ServiceSource.class)))
                    .thenReturn(Optional.empty());

            when(rateLimitingRepository.saveAndFlush(any(RateLimiting.class))).thenAnswer(inv -> inv.getArgument(0));

            rateLimitingService.rateLimiting(RECIPIENT_ID, CHANNEL, EVENT_TYPE, SOURCE);

            // Layer 1: recipient + channel + eventType + GLOBAL
            verify(rateLimitingRepository)
                    .findByRecipientIdAndChannelAndEventTypeAndSource(
                            RECIPIENT_ID, CHANNEL, EVENT_TYPE, ServiceSource.GLOBAL);

            // Layer 2: recipient + channel + GLOBAL + source
            verify(rateLimitingRepository)
                    .findByRecipientIdAndChannelAndEventTypeAndSource(RECIPIENT_ID, CHANNEL, EventType.GLOBAL, SOURCE);

            // Layer 3: recipient + channel + GLOBAL + GLOBAL
            verify(rateLimitingRepository)
                    .findByRecipientIdAndChannelAndEventTypeAndSource(
                            RECIPIENT_ID, CHANNEL, EventType.GLOBAL, ServiceSource.GLOBAL);
        }
    }

    private RateLimiting buildRecord(EventType eventType, ServiceSource source, LocalDateTime now) {
        return RateLimiting.builder()
                .recipientId(RECIPIENT_ID)
                .channel(CHANNEL)
                .eventType(eventType)
                .source(source)
                .sendCount(2)
                .windowEnd(now.plusMinutes(30))
                .lastSendAt(now.minusSeconds(120))
                .build();
    }

    private RateLimiting buildExpiredRecord(EventType eventType, ServiceSource source, LocalDateTime now) {
        return RateLimiting.builder()
                .recipientId(RECIPIENT_ID)
                .channel(CHANNEL)
                .eventType(eventType)
                .source(source)
                .sendCount(5)
                .windowEnd(now.minusMinutes(1))
                .lastSendAt(now.minusMinutes(70))
                .build();
    }
}
