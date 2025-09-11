package com.grow.notification_service.quiz.application.dto;

import java.util.List;

import com.grow.notification_service.quiz.domain.model.Quiz;

public record QuizItem(Long quizId, String question, List<String> choices, String level, Long categoryId) {
	public static QuizItem from(Quiz q) {
		return new QuizItem(q.getQuizId(), q.getQuestion(), q.getChoices(), q.getLevel().name(), q.getCategoryId());
	}
}