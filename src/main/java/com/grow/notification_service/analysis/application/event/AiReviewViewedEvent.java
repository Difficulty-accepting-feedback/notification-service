package com.grow.notification_service.analysis.application.event;

import java.util.List;

public record AiReviewViewedEvent(
	Long memberId,
	Long categoryId,
	String sessionId,
	List<Long> quizIds
) {}