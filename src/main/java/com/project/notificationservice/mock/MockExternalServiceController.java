package com.project.notificationservice.mock;

import com.project.notificationservice.dto.ApiResponse;
import com.project.notificationservice.dto.NotificationEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/external-service")
public class MockExternalServiceController {

    private final RabbitNotificationPublisher rabbitNotificationPublisher;

    @PostMapping("/notifications/publish")
    public ResponseEntity<?> publish(@RequestBody @Valid NotificationEvent event) {

        // @Valid validate tại tầng controller trước khi publish message cho broker
        rabbitNotificationPublisher.publish(event);

        return ResponseEntity.ok(ApiResponse.success("Published successfully"));
    }
}
