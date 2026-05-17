package com.project.notificationservice.controller;

import com.project.notificationservice.dto.NotificationEvent;
import com.project.notificationservice.dto.response.ApiResponse;
import com.project.notificationservice.publisher.NotificationPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController  {

    private final NotificationPublisher notificationPublisher;

    @PostMapping("/publish")
    public ResponseEntity<?> publish(@RequestBody @Valid NotificationEvent event) {
        // @Valid trigger validation annotations trong NotificationEvent
        notificationPublisher.publish(event);
        return ResponseEntity.ok(ApiResponse.success("Published successfully"));
    }
}
