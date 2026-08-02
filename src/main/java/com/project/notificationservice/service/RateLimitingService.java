package com.project.notificationservice.service;

import com.project.notificationservice.enums.Channel;
import com.project.notificationservice.enums.EventType;
import com.project.notificationservice.enums.ServiceSource;

public interface RateLimitingService {
    void rateLimiting(String recipientId, Channel channel, EventType eventType, ServiceSource source);
}
