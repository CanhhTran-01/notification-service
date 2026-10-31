package com.project.notificationservice.dto;

import com.project.notificationservice.enums.Channel;
import com.project.notificationservice.enums.EventType;
import com.project.notificationservice.enums.ServiceSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;

@Data
public class CreateNotificationRequest {
    @NotBlank
    private String eventId;

    @NotNull
    private ServiceSource source;

    private String recipientId;

    @NotBlank
    private String recipientContact;

    @NotNull
    private Channel channel;

    @NotNull
    private EventType eventType;

    private Map<String, Object> payload;
}
