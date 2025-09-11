package com.grow.notification_service.quiz.application.event;

import java.time.LocalDateTime;

public record QuizAnsweredEvent(
	Long memberId,
	Long quizId,
	Long categoryId,
	String level,
	String submitted,
	boolean correct,
	LocalDateTime occurredAt
) {
}