package com.grow.notification_service.global.exception;

import lombok.Getter;

@Getter
public class QuizException extends ServiceException {
	public QuizException(ErrorCode errorCode) {
		super(errorCode);
	}

	public QuizException(ErrorCode errorCode, Throwable cause) {
		super(errorCode);
		initCause(cause);
	}
}