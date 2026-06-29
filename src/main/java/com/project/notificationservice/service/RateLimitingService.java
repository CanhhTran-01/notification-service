package com.project.notificationservice.service;

import com.project.notificationservice.domain.enums.Channel;
import com.project.notificationservice.domain.enums.EventType;
import com.project.notificationservice.domain.enums.ServiceSource;

public interface RateLimitingService {
    void rateLimiting(String recipientId, Channel channel, EventType eventType, ServiceSource source);
}
