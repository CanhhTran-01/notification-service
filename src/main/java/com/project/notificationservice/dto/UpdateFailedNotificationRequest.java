package com.project.notificationservice.dto;

import java.util.Map;
import lombok.Data;

@Data
public class UpdateFailedNotificationRequest {
    private String recipientContact;
    private Map<String, Object> payload;
    private String errorMessage;
}
