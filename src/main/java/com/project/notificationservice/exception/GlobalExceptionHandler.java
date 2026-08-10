package com.project.notificationservice.exception;

import com.project.notificationservice.dto.ApiResponse;
import java.io.IOException;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String SSE_PATH_SEGMENT = "/stream/";

    // Xử lý riêng cho exception phát sinh từ SSE/Async streaming
    // (client tự ngắt kết nối, timeout do EventSource tự reconnect...)
    @ExceptionHandler({
            AsyncRequestTimeoutException.class, // ít xảy ra do SseEmitter(0L)
            ClientAbortException.class, // Tomcat ném khi client đóng kết nối đột ngột
            IOException.class
    })
    public ResponseEntity<ApiResponse<?>> handleAsyncStreamException(Exception exception, HttpServletRequest request) {

        // chỉ xét request URI chứa "/stream/"
        if (request.getRequestURI().contains(SSE_PATH_SEGMENT)) {

            // đây là exception bình thường của SSE (client tự ngắt/reconnect), chỉ log
            log.debug("SSE stream interrupted at [{}]: {}", request.getRequestURI(), exception.getMessage());

            // trả về response rỗng
            return ResponseEntity.status(HttpStatus.OK).build();
        }

        // Đây là Exception xảy ra ở API khác không phải SSE, log error để giám sát
        log.error("Unexpected I/O error at [{}]: {}", request.getRequestURI(), exception.getMessage(), exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.UNEXPECTED_ERROR));
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<?> handlingBaseException(BaseException exception) {
        log.error(
                "Business Exception: code={}, message={}",
                exception.getErrorCode().getCode(),
                exception.getMessage());

        return ResponseEntity.status(exception.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(exception.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handlingMethodArgumentNotValidException(MethodArgumentNotValidException exception) {

        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", detail);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(detail));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handlingRuntimeException(Exception exception) {

        log.error("Unexpected Exception: ", exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.UNEXPECTED_ERROR));
    }
}
