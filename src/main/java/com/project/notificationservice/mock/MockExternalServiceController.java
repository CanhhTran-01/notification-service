package com.project.notificationservice.mock;

import com.project.notificationservice.dto.ApiResponse;
import com.project.notificationservice.dto.NotificationEvent;
import com.project.notificationservice.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/external-service")
public class MockExternalServiceController {

    private final MockExternalServicePublisher mockExternalServicePublisher;
    private final NotificationPreferenceService preferenceService;

    @PostMapping("/notifications/publish")
    public ResponseEntity<?> publish(@RequestBody @Valid NotificationEvent event) {

        // @Valid trigger validation annotations trong NotificationEvent
        mockExternalServicePublisher.publish(event);
        return ResponseEntity.ok(ApiResponse.success("Published successfully"));
    }

    @GetMapping("/preferences/{recipientId}/all-channels")
    public ResponseEntity<?> getAllChannels(@PathVariable String recipientId) {

        var apiResponse = ApiResponse.success(preferenceService.getAllChannels(recipientId));
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/preferences/{recipientId}/active-channels")
    public ResponseEntity<?> getActiveChannels(@PathVariable String recipientId) {

        var apiResponse = ApiResponse.success(preferenceService.getActiveChannels(recipientId));
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/preferences/{recipientId}/{channel}")
    public ResponseEntity<?> updateChannelPreference(@PathVariable String recipientId, @PathVariable String channel) {

        preferenceService.updateChannelPreference(recipientId, channel);
        return ResponseEntity.noContent().build();
    }
}
