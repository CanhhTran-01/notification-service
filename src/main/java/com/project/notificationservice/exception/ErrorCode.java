package com.project.notificationservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 1xxx - General Errors
    UNEXPECTED_ERROR(1000, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR(1001, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    SYSTEM_BUSY(1002, "Hệ thống đang bận, vui lòng thử lại sau", HttpStatus.SERVICE_UNAVAILABLE),

    // 2xxx - Notification Errors
    UNSUPPORTED_CHANNEL(2001, "Kênh không hỗ trợ", HttpStatus.BAD_REQUEST),
    NOTIFICATION_NOT_FOUND(2002, "Không tìm thấy notification", HttpStatus.NOT_FOUND),
    TEMPLATE_NOT_FOUND(2003, "Không tìm thấy template tương ứng", HttpStatus.NOT_FOUND),
    PREFERENCE_NOT_FOUND(2004, "Không tìm thấy preference", HttpStatus.NOT_FOUND),
    DEAD_LETTER_NOT_FOUND(2005, "Không tìm thấy dead letter", HttpStatus.NOT_FOUND),
    REQUEST_TOO_FREQUENT(2006, "Thao tác quá nhanh, vui lòng không spam!", HttpStatus.TOO_MANY_REQUESTS),
    MESSAGE_LIMIT_EXCEEDED(2007, "Vượt quá giới hạn gửi tin!", HttpStatus.TOO_MANY_REQUESTS),
    INVALID_DEAD_LETTER_PAYLOAD(2008, "Dead letter không thể deserialize!", HttpStatus.BAD_REQUEST),

    // 3xxx - External Service Errors
    EMAIL_SERVICE_ERROR(3000, "Lỗi khi gửi email", HttpStatus.SERVICE_UNAVAILABLE),
    SMS_SERVICE_ERROR(3001, "Lỗi khi gửi SMS", HttpStatus.SERVICE_UNAVAILABLE),
    PUSH_SERVICE_ERROR(3002, "Lỗi khi gửi push notification", HttpStatus.SERVICE_UNAVAILABLE),

    // 4xxx — Message Broker Errors
    MESSAGE_BROKER_ERROR(4000, "Lỗi khi gửi message", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
