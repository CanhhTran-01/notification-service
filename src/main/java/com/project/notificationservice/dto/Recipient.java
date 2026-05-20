package com.project.notificationservice.dto;

import com.project.notificationservice.domain.enums.Channel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Recipient {
    private String userId;
    private String email;
    private String phone;
    private String deviceToken;

    // mapping each channel -> corresponding recipient contact
    public String getContactByChannel(Channel channel) {
        String contact =
                switch (channel) {
                    case EMAIL -> this.email;
                    case SMS -> this.phone;
                    case PUSH -> this.deviceToken;
                    case IN_APP -> this.userId;
                };

        if (contact == null || contact.isBlank()) {
            throw new IllegalArgumentException("Recipient has no contact for channel: " + channel);
        }

        return contact;
    }
}
