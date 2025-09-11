package com.grow.notification_service.quiz.application.seed;

import java.util.List;

public record QuizSeedDto(
	String question,
	List<String> choices,
	String answer,
	String explain,
	String level,
	Long categoryId
) {}