package com.project.notificationservice.controller;

import com.project.notificationservice.dto.ApiResponse;
import com.project.notificationservice.service.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminNotificationService adminNotificationService;

    // admin retry lại thông báo FAILED
    @PostMapping("/notifications/{id}/retry")
    public ResponseEntity<ApiResponse<?>> retryMessage(@PathVariable String id) {

        adminNotificationService.retry(id);
        return ResponseEntity.noContent().build();
    }

    // admin replay lại thông báo từ DLQ
    @PostMapping("/dead-letter-events/{id}/replay")
    public ResponseEntity<ApiResponse<?>> replayMessage(@PathVariable String id, @RequestParam String queueName) {

        adminNotificationService.replay(id, queueName);
        return ResponseEntity.noContent().build();
    }

    // admin lấy các notifications FAILED cho UI "lịch sử gửi thất bại"     GET  /api/v1/admin/notifications/failed
    // admin lấy các notifications trong DLQ cho UI "sự kiện chưa xử lý"    GET  /api/v1/admin/dead-letter-events
    // admin tạo thông báo ở trang thái DRAF                                POST /api/v1/admin/notifications
    // admin lên lịch/cập nhật lịch thông báo                               POST /api/v1/admin/notifications/{id}/schedule
    // admin update thông báo failed để gọi api retry                       PUT  /api/v1/admin/notifications/{id}
}
