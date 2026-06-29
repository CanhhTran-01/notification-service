package com.project.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    // admin lấy các notifications FAILED cho UI "lịch sử gửi thất bại"     GET  /api/v1/admin/notifications/failed
    // admin lấy các notifications trong DLQ cho UI "sự kiện chưa xử lý"    GET  /api/v1/admin/dead-letter-events
    // admin tạo thông báo ở trang thái DRAF                                POST /api/v1/admin/notifications
    // admin lên lịch/cập nhật lịch thông báo                               POST /api/v1/admin/notifications/{id}/schedule
    // admin update thông báo failed để gọi api retry                       PUT  /api/v1/admin/notifications/{id}

    // admin retry lại thông báo FAILED                                     POST /api/v1/admin/notifications/{id}/retry
    // admin replay lại thông báo từ DLQ                                    POST /api/v1/admin/dead-letter-events/{id}/replay
}
