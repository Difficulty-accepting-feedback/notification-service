package com.grow.notification_service.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	// 쪽지
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "404-0", "member.not.found"),
	MEMBER_RESOLVE_FAILED(HttpStatus.BAD_REQUEST, "400-1", "member.resolve.failed"),
	MEMBER_RESOLVE_EMPTY(HttpStatus.BAD_REQUEST, "400-2", "member.resolve.empty");
	private final HttpStatus status;
	private final String code;
	private final String messageCode;
}