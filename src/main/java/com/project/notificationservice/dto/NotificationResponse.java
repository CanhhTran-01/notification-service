package com.project.notificationservice.dto;

import com.project.notificationservice.domain.enums.EventType;
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
}
