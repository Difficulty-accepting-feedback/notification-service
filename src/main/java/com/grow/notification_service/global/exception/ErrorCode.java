package com.grow.notification_service.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {;

	private final HttpStatus status;
	private final String code;
	private final String messageCode;
}