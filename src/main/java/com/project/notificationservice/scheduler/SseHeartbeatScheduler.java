package com.project.notificationservice.scheduler;

import com.project.notificationservice.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseHeartbeatScheduler {

    private final SseEmitterService sseEmitterService;

    @Scheduled(fixedRateString = "${app.sse.heartbeat-interval-ms}")
    public void keepSseConnectionsAlive() {
        sseEmitterService.pingAllEmitters();
    }
}
