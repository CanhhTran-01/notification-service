package com.project.notificationservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeadLetterEventResponse {
    private UUID id;
    private String queueName;
    private String payload;
    private String errorReason;
    private int retryCount;
    private boolean resolved;
    private LocalDateTime createdAt;
}
