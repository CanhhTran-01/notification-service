package com.project.notificationservice.controller;

import com.project.notificationservice.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@CrossOrigin(originPatterns = "*", allowCredentials = "true") // dùng để test local/dev
public class SseNotificationController {

    private final SseEmitterService sseEmitterService;

    @GetMapping("/notifications/stream/subscribe/{recipientId}")
    public SseEmitter subscribe(@PathVariable String recipientId) {
        // user kết nối
        return sseEmitterService.subscribe(recipientId);
    }
}
