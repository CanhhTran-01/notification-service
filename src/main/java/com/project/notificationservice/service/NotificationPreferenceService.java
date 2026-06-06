package com.project.notificationservice.service;

import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.dto.PreferenceResponse;
import java.util.List;

public interface NotificationPreferenceService {

    List<PreferenceResponse> getAllChannels(String recipientId);

    List<PreferenceResponse> getActiveChannels(String recipientId);

    void updateChannelPreference(String recipientId, String channel);

    boolean isChannelEnabled(String recipientId, Channel channel);
}
