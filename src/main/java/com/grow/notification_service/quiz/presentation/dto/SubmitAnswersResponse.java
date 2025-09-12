package com.grow.notification_service.quiz.presentation.dto;

import java.util.List;

public record SubmitAnswersResponse(
	int total, int correctCount, List<SubmitAnswerResult> results
) {}