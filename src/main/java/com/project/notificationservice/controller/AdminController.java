package com.project.notificationservice.controller;

import com.project.notificationservice.dto.ApiResponse;
import com.project.notificationservice.dto.CreateNotificationRequest;
import com.project.notificationservice.dto.ScheduleNotificationRequest;
import com.project.notificationservice.dto.UpdateFailedNotificationRequest;
import com.project.notificationservice.service.AdminNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    // admin lấy các notifications FAILED
    @GetMapping("/notifications/failed")
    public ResponseEntity<ApiResponse<?>> getFailedNotifications(
            @RequestParam(defaultValue = "0") int pageNumber, @RequestParam(defaultValue = "10") int pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return ResponseEntity.ok(ApiResponse.success(adminNotificationService.getFailedNotifications(pageable)));
    }

    // admin lấy các dead-letter events theo tiêu chí
    @GetMapping("/dead-letter-events")
    public ResponseEntity<ApiResponse<?>> searchDeadLetterEvents(
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) String queueName,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return ResponseEntity.ok(
                ApiResponse.success(adminNotificationService.searchDeadLetterEvents(resolved, queueName, pageable)));
    }

    // admin tạo thông báo DRAFT
    @PostMapping("/notifications")
    public ResponseEntity<ApiResponse<?>> createDraft(@Valid @RequestBody CreateNotificationRequest request) {

        return ResponseEntity.ok(ApiResponse.success(adminNotificationService.createDraft(request)));
    }

    // admin lên lịch / cập nhật lịch
    @PostMapping("/notifications/{id}/schedule")
    public ResponseEntity<ApiResponse<?>> schedule(
            @PathVariable String id, @Valid @RequestBody ScheduleNotificationRequest request) {

        return ResponseEntity.ok(ApiResponse.success(adminNotificationService.schedule(id, request)));
    }

    // admin update thông báo FAILED trước khi retry
    @PutMapping("/notifications/{id}")
    public ResponseEntity<ApiResponse<?>> updateFailed(
            @PathVariable String id, @RequestBody UpdateFailedNotificationRequest request) {

        return ResponseEntity.ok(ApiResponse.success(adminNotificationService.updateFailed(id, request)));
    }
}
