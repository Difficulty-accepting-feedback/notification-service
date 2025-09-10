package com.grow.notification_service.note.application.event;

import java.time.LocalDateTime;

public record NoteNotificationEvent(
	Long memberId,
	String code,
	String notificationType,
	String title,
	String content,
	Long noteId,
	LocalDateTime occurredAt
) {
}