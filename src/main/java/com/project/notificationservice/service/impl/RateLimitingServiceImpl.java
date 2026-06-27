package com.project.notificationservice.service.impl;

import com.project.notificationservice.config.properties.RateLimitProperties;
import com.project.notificationservice.domain.entity.RateLimiting;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.domain.enums.EventType;
import com.project.notificationservice.domain.enums.ServiceSource;
import com.project.notificationservice.exception.ErrorCode;
import com.project.notificationservice.exception.RateLimitingException;
import com.project.notificationservice.repository.RateLimitingRepository;
import com.project.notificationservice.service.RateLimitingService;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitingServiceImpl implements RateLimitingService {

    private final RateLimitingRepository rateLimitingRepository;
    private final RateLimitProperties rateLimitProperties;

    @Override
    @Transactional
    public void rateLimiting(String recipientId, Channel channel, EventType eventType, ServiceSource source) {

        // layer 1: recipient + channel + eventType
        limitByPattern(recipientId, channel, eventType, ServiceSource.GLOBAL);

        // layer 2: recipient + channel + source
        limitByPattern(recipientId, channel, EventType.GLOBAL, source);

        // layer 3: recipient + channel
        limitByPattern(recipientId, channel, EventType.GLOBAL, ServiceSource.GLOBAL);
    }

    private void limitByPattern(String recipientId, Channel channel, EventType eventType, ServiceSource source) {

        // lấy ra values từ yml file và check
        var limitsMap = rateLimitProperties.getLimits();
        if (limitsMap == null
                || limitsMap.isEmpty()
                || !limitsMap.containsKey(channel.name().toLowerCase())) {

            log.info("No rate limit config for channel={}", channel);
            return; // Không cấu hình pattern này thì không chặn
        }

        RateLimitProperties.ChannelLimit config =
                limitsMap.get(channel.name().toLowerCase()); // lấy config tương ứng channel
        LocalDateTime now = LocalDateTime.now(); // lấy time now

        // tìm record ở DB và pessimistic Lock lại (xử lý case spam cùng lúc)
        RateLimiting record = rateLimitingRepository
                .findByRecipientIdAndChannelAndEventTypeAndSource(
                        recipientId, channel, eventType, source) // luôn lock -> tốn 1 round-trip
                .orElse(null); // không tìm thấy record

        if (record == null) {

            // chưa có record -> tạo mới với sendCount = 1
            try {
                RateLimiting newRecord = RateLimiting.builder()
                        .recipientId(recipientId)
                        .channel(channel)
                        .eventType(eventType)
                        .source(source)
                        .windowEnd(now.plusMinutes(config.getWindowMinutes())) // end = now + window-minutes
                        .lastSendAt(now)
                        .build();

                rateLimitingRepository.saveAndFlush(
                        newRecord); // saveAndFlush(): đồng bộ hóa Persistence Context -> catch exception

            } catch (DataIntegrityViolationException exception) {

                // handling concurrency: thread khác nhanh chân insert trước -> load lại record để xử lý
                record = rateLimitingRepository
                        .findByRecipientIdAndChannelAndEventTypeAndSource(recipientId, channel, eventType, source)
                        .orElseThrow(() -> new RateLimitingException(ErrorCode.SYSTEM_BUSY));

                processExistingRecord(record, now, config); // đã có record -> check + update
            }

        } else {

            // đã có record -> check + update
            processExistingRecord(record, now, config);
        }
    }

    private void processExistingRecord(
            RateLimiting record, LocalDateTime now, RateLimitProperties.ChannelLimit config) {

        // luật giãn cách: lần sau phải cách lần trước ít nhất X seconds
        if (config.getMinIntervalSeconds() > 0) {

            long secondSinceLastSend =
                    Duration.between(record.getLastSendAt(), now).toSeconds();

            if (secondSinceLastSend < config.getMinIntervalSeconds()) {

                log.warn(
                        "Request too frequent: recipientId={}, channel={}, secondsSinceLastSend={}",
                        record.getRecipientId(),
                        record.getChannel(),
                        secondSinceLastSend);

                throw new RateLimitingException(ErrorCode.REQUEST_TOO_FREQUENT);
            }
        }

        // luật số lượng: trong vòng Y seconds chỉ được gửi Z tin
        if (now.isAfter(record.getWindowEnd())) {
            // hết chu kì, reset window duration
            record.resetWindow(now.plusMinutes(config.getWindowMinutes()));

        } else {
            // còn chu kì, check send_count
            if (record.getSendCount() >= config.getMaxCount()) {
                throw new RateLimitingException(ErrorCode.MESSAGE_LIMIT_EXCEEDED);
            }

            record.recordSend();
        }
    }
}
