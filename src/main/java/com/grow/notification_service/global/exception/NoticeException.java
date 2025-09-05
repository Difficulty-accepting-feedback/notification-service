package com.grow.notification_service.global.exception;

public class NoticeException extends ServiceException {
	public NoticeException(ErrorCode errorCode) {
		super(errorCode);
	}

	public NoticeException(ErrorCode errorCode, Throwable cause) {
		super(errorCode);
		initCause(cause);
	}
}