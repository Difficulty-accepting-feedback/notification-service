package com.grow.notification_service.note.domain.exception;

import com.grow.notification_service.common.exception.DomainException;

public class NoteDomainException extends DomainException {

	public NoteDomainException(String message) {
		super(message);
	}

	public static NoteDomainException senderIdRequired() {
		return new NoteDomainException("senderId는 null일 수 없습니다.");
	}

	public static NoteDomainException recipientIdRequired() {
		return new NoteDomainException("recipientId는 null일 수 없습니다.");
	}

	public static NoteDomainException selfSendNotAllowed() {
		return new NoteDomainException("자기 자신에게는 쪽지를 보낼 수 없습니다.");
	}

	public static NoteDomainException emptyContent() {
		return new NoteDomainException("쪽지 내용은 비어있을 수 없습니다.");
	}

	public static NoteDomainException invalidCreatedAt() {
		return new NoteDomainException("유효하지 않은 생성 시각입니다.");
	}

	public static NoteDomainException readAllowedOnlyForRecipient() {
		return new NoteDomainException("수신자만 읽음 처리할 수 있습니다.");
	}

	public static NoteDomainException deletePermissionDenied() {
		return new NoteDomainException("삭제 권한이 없습니다.");
	}
}