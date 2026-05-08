package com.project.notificationservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    UNSUPPORTED_CHANNEL(9999, "Kênh không hỗ trợ.");

    private final int code;
    private final String message;
}
