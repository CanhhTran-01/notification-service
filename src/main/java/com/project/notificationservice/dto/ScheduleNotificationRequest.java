package com.project.notificationservice.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ScheduleNotificationRequest {
    @NotNull
    private LocalDateTime scheduledAt;
}
