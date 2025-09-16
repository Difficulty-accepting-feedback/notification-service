package com.grow.notification_service.global.exception;

public class AnalysisException extends ServiceException {
	public AnalysisException(ErrorCode errorCode) {
		super(errorCode);
	}

	public AnalysisException(ErrorCode errorCode, Throwable cause) {
		super(errorCode);
		initCause(cause);
	}
}