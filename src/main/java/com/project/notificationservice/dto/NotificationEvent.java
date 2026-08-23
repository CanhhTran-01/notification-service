package com.project.notificationservice.dto;

import com.project.notificationservice.enums.Channel;
import com.project.notificationservice.enums.EventType;
import com.project.notificationservice.enums.ServiceSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class NotificationEvent {

    // Header - send to ?
    @NotBlank
    private String eventId; // idempotency check        - ok

    private EventType eventType; // pick template            - ok
    private ServiceSource source; // logging/monitoring       - ok
    private LocalDateTime timestamp; // audit log

    // Recipient - who ?
    @NotNull
    private Recipient recipient;

    // Channel - through which channel ?
    @NotEmpty
    private Set<Channel> channels;

    // Payload - message content
    @NotNull
    private Map<String, Object> payload;
}
