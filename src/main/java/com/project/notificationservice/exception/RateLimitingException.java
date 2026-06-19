package com.project.notificationservice.exception;

import lombok.Getter;

@Getter
public class RateLimitingException extends BaseException {

    public RateLimitingException(ErrorCode errorCode) {
        super(errorCode);
    }
}
