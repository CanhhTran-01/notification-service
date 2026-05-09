package com.project.notificationservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Recipient {
    private String userId;
    private String email;
    private String phone;
    private String deviceToken;
}
