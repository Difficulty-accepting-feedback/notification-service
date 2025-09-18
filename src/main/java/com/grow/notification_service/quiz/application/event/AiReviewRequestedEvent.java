package com.grow.notification_service.quiz.application.event;

import java.time.LocalDateTime;

public record AiReviewRequestedEvent(
	Long memberId,
	Long categoryId,
	String levelParam,
	String topic,
	String dedupeKey,
	LocalDateTime requestedAt
) {}