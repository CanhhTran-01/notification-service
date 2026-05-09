package com.project.notificationservice.dto;

import com.project.notificationservice.domain.enums.Channel;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationEvent {

    // Header - send to ?
    private String eventId; // idempotency check
    private String eventType; // pick template
    private String source; // logging/monitoring
    private String version; // backward compatibility
    private LocalDateTime timestamp; // audit log

    // Recipient - who receive ?
    private Recipient recipient;

    // Channel - through which channel ?
    private Set<Channel> channels;

    // Payload - message content
    private Map<String, Object> payload;
}
