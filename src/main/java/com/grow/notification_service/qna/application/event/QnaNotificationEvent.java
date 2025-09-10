package com.grow.notification_service.qna.application.event;

import java.time.LocalDateTime;

import com.grow.notification_service.chatbot.infra.persistence.enums.QNA;

public record QnaNotificationEvent(
	Long memberId,
	String code,
	String notificationType,
	String title,
	String content,
	Long answerId,
	LocalDateTime occurredAt
) {
}