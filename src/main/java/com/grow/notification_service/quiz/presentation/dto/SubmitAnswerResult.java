package com.grow.notification_service.quiz.presentation.dto;

public record SubmitAnswerResult(
	Long quizId,
	boolean correct,
	String submitted,
	String correctAnswer,
	String explain,
	String level,
	Long categoryId
) {}