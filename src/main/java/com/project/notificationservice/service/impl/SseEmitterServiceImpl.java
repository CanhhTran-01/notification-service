package com.project.notificationservice.service.impl;

import com.project.notificationservice.service.SseEmitterService;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Slf4j
public class SseEmitterServiceImpl implements SseEmitterService {

    // scope hiện tại: chỉ cho phép 1 tab nhận IN_APP notification tại 1 thời điểm cho mỗi recipientId
    // Lưu các kết nối user theo recipientId: Key = recipientId, Value = SseEmitter
    // nếu cần multi-device support -> Map<String, List<SseEmitter>>
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private SseEmitter createEmitter(String recipientId) {
        SseEmitter emitter = new SseEmitter(0L); // timeout vô hạn - kết nối vĩnh viễn cho đến khi đóng

        // TODO: Dùng emitters.remove(key, value) hay emitters.remove(key) ?

        // khi kết nối đóng hoàn toàn
        emitter.onCompletion(() -> {
            emitters.remove(recipientId);
            log.info("SSE connection closed (completed) for recipientId={}", recipientId);
        });

        // khi kết nối quá time cho phép
        emitter.onTimeout(() -> {
            emitters.remove(recipientId);
            log.info("SSE connection timed out for recipientId={}", recipientId);
        });

        // khi có lỗi: mạng đứt, lỗi I/O khi truyền,...
        emitter.onError((e) -> {
            emitters.remove(recipientId);
            log.warn("SSE connection error for recipientId={}: {}", recipientId, e.getMessage());
        });

        return emitter;
    }

    @Override
    public SseEmitter subscribe(String recipientId) {

        // checking
        SseEmitter oldEmitter = emitters.get(recipientId); // tồn tại -> overwrite gây ra emitter "zombie"
        if (oldEmitter != null) {
            oldEmitter.complete(); // đóng kết nối cũ trước khi thay bằng cái mới
        }

        // khởi tạo và cấu hình Emitter mới
        SseEmitter emitter = createEmitter(recipientId);

        emitters.put(recipientId, emitter);
        log.info("New SSE connection established for recipientId={}", recipientId);

        return emitter;
    }

    @Override
    public void sendToUser(String recipientId, Object payload) {
        SseEmitter emitter = emitters.get(recipientId);

        // user ngắt kết nối -> return
        if (emitter == null) {
            log.info("User [{}] is not online", recipientId);
            return;
        }

        // user còn kết nối -> gửi
        synchronized (emitter) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));

            } catch (IOException e) {
                log.warn("Failed to send SSE to recipientId={}: {}", recipientId, e.getMessage());
                emitters.remove(recipientId);
            }
        }
    }

    // heartbeat pattern with scheduler
    @Override
    public void pingAllEmitters() {
        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            SseEmitter emitter = entry.getValue();
            synchronized (emitter) {
                try {
                    entry.getValue().send(SseEmitter.event().name("ping").data("keep-alive"));
                    log.debug("Heartbeat sent to recipientId={}", entry.getKey());

                } catch (IOException e) {
                    log.debug(
                            "Heartbeat failed, removing dead connection: recipientId={}, error={}",
                            entry.getKey(),
                            e.getMessage());
                    emitters.remove(entry.getKey());
                }
            }
        }
    }
}
