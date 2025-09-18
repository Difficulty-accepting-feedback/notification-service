package com.grow.notification_service.analysis.presentation.controller.dto;

import java.util.List;

import com.grow.notification_service.quiz.domain.model.Quiz;

public record QuizResponse(
	Long quizId,
	String question,
	List<String> choices,
	String answer,
	String explain,
	String level,
	Long categoryId
) {
	public static QuizResponse from(Quiz q) {
		return new QuizResponse(
			q.getQuizId(),
			q.getQuestion(),
			q.getChoices(),
			q.getAnswer(),
			q.getExplain(),
			q.getLevel().name(),
			q.getCategoryId()
		);
	}
}