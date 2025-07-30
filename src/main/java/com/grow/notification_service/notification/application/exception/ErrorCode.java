package com.grow.notification_service.notification.application.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SSE_NOT_CONNECTED("400", "SSE 연결이 되어있지 않습니다."),
    ;

    private final String code;
    private final String message;
}
