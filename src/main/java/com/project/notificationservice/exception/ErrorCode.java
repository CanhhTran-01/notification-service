package com.project.notificationservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 1xxx - General Errors
    UNEXPECTED_ERROR(1000, "Lỗi hệ thống"),

    // 2xxx - Notification Errors
    UNSUPPORTED_CHANNEL(9999, "Kênh không hỗ trợ.");

    private final int code;
    private final String message;
}
