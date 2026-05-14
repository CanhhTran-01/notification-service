package com.project.notificationservice.exception;

import com.project.notificationservice.dto.response.ApiResponse;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

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
