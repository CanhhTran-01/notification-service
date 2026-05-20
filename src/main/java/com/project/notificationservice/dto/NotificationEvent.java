package com.project.notificationservice.dto;

import com.project.notificationservice.domain.enums.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationEvent {

    // Header - send to ?
    @NotBlank
    private String eventId; // idempotency check

    private String eventType; // pick template
    private String source; // logging/monitoring
    private String version; // backward compatibility
    private LocalDateTime timestamp; // audit log

    // Recipient - who receive ?
    @NotNull
    private Recipient recipient;

    // Channel - through which channel ?
    @NotEmpty
    private Set<Channel> channels;

    // Payload - message content
    @NotNull
    private Map<String, Object> payload;
}
