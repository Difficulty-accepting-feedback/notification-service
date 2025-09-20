package com.grow.notification_service.analysis.application.event;

import java.time.LocalDateTime;

import com.grow.notification_service.notification.infra.persistence.entity.NotificationType;

public record AnalysisNotificationEvent(
	Long memberId,
	NotificationType notificationType,
	String content,
	LocalDateTime createdAt
) {}