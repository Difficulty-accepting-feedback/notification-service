package com.grow.notification_service.qna.domain.exception;

import com.grow.notification_service.common.exception.DomainException;

public class QnaDomainException extends DomainException {

	public QnaDomainException(String message) {
		super(message);
	}

	public static QnaDomainException AnswerParentIdRequired() {
		return new QnaDomainException("ANSWER는 parentId가 필요합니다.");
	}

	public static QnaDomainException ContentCannotBeBlank() {
		return new QnaDomainException("내용은 비어 있을 수 없습니다.");
	}
}