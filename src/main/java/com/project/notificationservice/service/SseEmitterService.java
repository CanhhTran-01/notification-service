package com.project.notificationservice.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseEmitterService {
    SseEmitter subscribe(String recipientId);

    void sendToUser(String recipientId, Object payload);

    void pingAllEmitters();
}
