package com.grow.notification_service.global.exception;

import lombok.Getter;

@Getter
public class QnaException extends ServiceException {
	public QnaException(ErrorCode errorCode) {
		super(errorCode);
	}

	public QnaException(ErrorCode errorCode, Throwable cause) {
		super(errorCode);
		initCause(cause);
	}
}