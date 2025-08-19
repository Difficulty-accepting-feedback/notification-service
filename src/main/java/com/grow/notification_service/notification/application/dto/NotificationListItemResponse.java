package com.grow.notification_service.notification.application.dto;

import java.time.LocalDateTime;

import com.grow.notification_service.notification.infra.persistence.entity.NotificationType;

import lombok.Getter;

@Getter
public class NotificationListItemResponse {
	private final Long id;
	private final String title;       // [GROW], [댓글] 등
	private final String content;
	private final boolean read;
	private final LocalDateTime createdAt;

	public NotificationListItemResponse(
		Long id,
		NotificationType type,
		String content,
		Boolean isRead,
		LocalDateTime createdAt
	) {
		this.id = id;
		this.title = type != null ? type.getTitle() : "[GROW]";
		this.content = content;
		this.read = Boolean.TRUE.equals(isRead);
		this.createdAt = createdAt;
	}
}