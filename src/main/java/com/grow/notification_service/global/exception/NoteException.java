package com.grow.notification_service.global.exception;

import lombok.Getter;

@Getter
public class NoteException extends ServiceException {
	public NoteException(ErrorCode errorCode) {
		super(errorCode);
	}

	public NoteException(ErrorCode errorCode, Throwable cause) {
		super(errorCode);
		initCause(cause);
	}
}