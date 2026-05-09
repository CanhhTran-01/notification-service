package com.project.notificationservice.dto;

import com.project.notificationservice.domain.enums.Channel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Data
@Builder
public class NotificationEvent {

    // Header - send to ?
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private String source;
    private String version;

    // Recipient - who receive ?
    private Recipient recipient;

    // Channel - through which channel ?
    private Set<Channel> channels;

    // Payload - message content
    private Map<String, Object> payload;
}
