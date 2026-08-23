package com.project.notificationservice.controller;

import com.project.notificationservice.dto.ApiResponse;
import com.project.notificationservice.dto.NotificationEvent;
import com.project.notificationservice.publisher.RabbitNotificationPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/external-service")
public class NotificationPublisherController {

    private final RabbitNotificationPublisher rabbitNotificationPublisher;

    @PostMapping("/notifications/publish")
    public ResponseEntity<?> publish(@RequestBody @Valid NotificationEvent event) {
        rabbitNotificationPublisher.dispatch(event);
        return ResponseEntity.accepted().body(ApiResponse.success("Published successfully"));
    }

    @PostMapping("/notifications/publish-email")
    public ResponseEntity<?> publishEmail(@RequestBody @Valid NotificationEvent event) {
        // validate tại tầng controller trước khi publish message cho broker
        rabbitNotificationPublisher.publish(event); // EMAIL-PUSH-SMS
        return ResponseEntity.accepted().body(ApiResponse.success("Published successfully"));
    }

    @PostMapping("/notifications/publish-inapp")
    public ResponseEntity<?> publishInApp(@RequestBody @Valid NotificationEvent event) {
        rabbitNotificationPublisher.publishInApp(event); // IN_APP
        return ResponseEntity.accepted().body(ApiResponse.success("Published successfully"));
    }
}
