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
	MEMBER_RESOLVE_EMPTY(HttpStatus.BAD_REQUEST, "400-2", "member.resolve.empty"),

	// Q&A
	MEMBER_CHECK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-0", "member.check.failed"),
	NO_PERMISSION_TO_WRITE_ANSWER(HttpStatus.FORBIDDEN, "403-0", "no.permission.to.write.answer"),
	QNA_NOT_FOUND(HttpStatus.NOT_FOUND, "404-1", "qna.not.found"),
	INVALID_QNA_PARENT(HttpStatus.BAD_REQUEST, "400-3", "invalid.qna.parent"),
	QNA_FORBIDDEN(HttpStatus.FORBIDDEN, "403-1", "qna.forbidden");
	private final HttpStatus status;
	private final String code;
	private final String messageCode;
}