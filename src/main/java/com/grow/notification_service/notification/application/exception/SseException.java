package com.grow.notification_service.notification.application.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SseException extends RuntimeException{
    private final ErrorCode errorCode;

    public SseException(ErrorCode errorCode,
                        Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }
}
