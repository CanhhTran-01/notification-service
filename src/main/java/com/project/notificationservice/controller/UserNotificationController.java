package com.project.notificationservice.controller;

import com.project.notificationservice.dto.ApiResponse;
import com.project.notificationservice.service.NotificationPreferenceService;
import com.project.notificationservice.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserNotificationController {

    private final UserNotificationService userNotificationService;
    private final NotificationPreferenceService preferenceService;

    // user lấy ra toàn bộ channels
    // Hiện tại tin tưởng client truyền recipientId — có rủi ro IDOR,
    // chấp nhận tạm thời cho scope demo project
    @GetMapping("/preferences/{recipientId}/all-channels")
    public ResponseEntity<?> getAllChannels(@PathVariable String recipientId) {

        var apiResponse = ApiResponse.success(preferenceService.getAllChannels(recipientId));
        return ResponseEntity.ok(apiResponse);
    }

    // user lấy ra các channels đang active
    // Hiện tại tin tưởng client truyền recipientId — có rủi ro IDOR,
    // chấp nhận tạm thời cho scope demo project
    @GetMapping("/preferences/{recipientId}/active-channels")
    public ResponseEntity<?> getActiveChannels(@PathVariable String recipientId) {

        var apiResponse = ApiResponse.success(preferenceService.getActiveChannels(recipientId));
        return ResponseEntity.ok(apiResponse);
    }

    // user click để turn on/off channel
    // Hiện tại tin tưởng client truyền recipientId — có rủi ro IDOR,
    // chấp nhận tạm thời cho scope demo project
    @PutMapping("/preferences/{recipientId}/{channel}")
    public ResponseEntity<?> updateChannelPreference(@PathVariable String recipientId, @PathVariable String channel) {

        preferenceService.updateChannelPreference(recipientId, channel);
        return ResponseEntity.noContent().build();
    }

    // user query lịch sử notification (luôn cho kênh IN_APP)
    // Hiện tại tin tưởng client truyền recipientId — có rủi ro IDOR,
    // chấp nhận tạm thời cho scope demo project
    @GetMapping("/notifications/{recipientId}")
    public ResponseEntity<?> getNotificationHistory(
            @PathVariable String recipientId,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "5") int pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        var apiResponse = ApiResponse.success(userNotificationService.getHistory(recipientId, pageable));
        return ResponseEntity.ok(apiResponse);
    }

    // user click vào thông báo để đọc -> update isRead
    // Hiện tại tin tưởng client truyền recipientId — rủi ro IDOR
    // chấp nhận tạm thời cho scope demo project
    @PutMapping("/notifications/{notificationId}/{recipientId}/read")
    public ResponseEntity<?> updateRead(@PathVariable String notificationId, @PathVariable String recipientId) {

        userNotificationService.updateRead(notificationId, recipientId);
        return ResponseEntity.noContent().build();
    }

    // Đếm số notification chưa đọc — hiển thị badge trên icon chuông/avatar
    // Hiện tại tin tưởng client truyền recipientId — có rủi ro IDOR,
    // chấp nhận tạm thời cho scope demo project
    @GetMapping("/notifications/{recipientId}/unread-count")
    public ResponseEntity<?> count(@PathVariable String recipientId) {

        var apiResponse = ApiResponse.success(userNotificationService.countNotReadNotification(recipientId));
        return ResponseEntity.ok(apiResponse);
    }
}
