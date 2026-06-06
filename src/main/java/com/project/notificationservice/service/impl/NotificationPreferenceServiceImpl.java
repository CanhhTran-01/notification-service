package com.project.notificationservice.service.impl;

import com.project.notificationservice.domain.entity.NotificationPreference;
import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.dto.PreferenceResponse;
import com.project.notificationservice.exception.BaseException;
import com.project.notificationservice.exception.ErrorCode;
import com.project.notificationservice.repository.NotificationPreferenceRepository;
import com.project.notificationservice.service.NotificationPreferenceService;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Override
    public List<PreferenceResponse> getAllChannels(String recipientId) {

        return preferenceRepository.findByRecipientId(recipientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<PreferenceResponse> getActiveChannels(String recipientId) {

        return preferenceRepository.findByRecipientIdAndIsEnabledTrue(recipientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void updateChannelPreference(String recipientId, String channel) {

        // handle exception
        try {
            Channel.valueOf(channel.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.info("Unsupported channel: {}", channel);
            throw new BaseException(ErrorCode.UNSUPPORTED_CHANNEL);
        }

        var preference = preferenceRepository
                .findByRecipientIdAndChannel(recipientId, Channel.valueOf(channel.toUpperCase()))
                .orElseThrow(() -> new BaseException(ErrorCode.PREFERENCE_NOT_FOUND));

        boolean oldValue = preference.isEnabled();
        preference.setEnabled(!oldValue); // update lại theo người dùng click

        log.info(
                "Preference updated: recipientId={}, channel={}, {} --> {}",
                recipientId,
                channel,
                oldValue ? "ON" : "OFF",
                preference.isEnabled() ? "ON" : "OFF");
    }

    @Override
    public boolean isChannelEnabled(String recipientId, Channel channel) {
        return preferenceRepository
                .findByRecipientIdAndChannel(recipientId, channel)
                .map(NotificationPreference::isEnabled)
                .orElse(true); // chưa có preference --> mặc định enabled
    }

    private PreferenceResponse toResponse(NotificationPreference entity) {
        return PreferenceResponse.builder()
                .recipientId(entity.getRecipientId())
                .channel(entity.getChannel())
                .isEnabled(entity.isEnabled())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
