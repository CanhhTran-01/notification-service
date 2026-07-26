package com.project.notificationservice.dto;

import com.project.notificationservice.domain.enums.Channel;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PreferenceResponse {
    private String recipientId;
    private Channel channel;
    private boolean isEnabled;
    private LocalDateTime updatedAt;
}
