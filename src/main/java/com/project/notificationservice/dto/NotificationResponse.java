package com.project.notificationservice.dto;

import com.project.notificationservice.enums.Channel;
import com.project.notificationservice.enums.EventType;
import com.project.notificationservice.enums.NotificationStatus;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private EventType eventType;
    private boolean isRead;
    private Map<String, Object> payload;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;

    // admin fields
    private String recipientId;
    private String recipientContact;
    private Channel channel;
    private NotificationStatus status;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime scheduledAt;
    private LocalDateTime nextRetryTime;
}
