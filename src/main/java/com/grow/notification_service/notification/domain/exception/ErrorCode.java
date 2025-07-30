package com.grow.notification_service.notification.domain.exception;

public enum ErrorCode {

    NOTIFICATION_NOT_FOUND("404", "존재하지 않는 알림입니다."),
    ;

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }
}
