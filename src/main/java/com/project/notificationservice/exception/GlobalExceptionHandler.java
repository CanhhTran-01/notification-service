package com.project.notificationservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public void handlingBaseException(BaseException exception) {
        log.error(
                "Business Exception: code={}, message={}",
                exception.getErrorCode().getCode(),
                exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public void handlingRuntimeException(Exception exception) {
        log.error("Exception bất định: ", exception);
    }
}
