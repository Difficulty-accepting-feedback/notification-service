package com.grow.notification_service.quiz.presentation.dto;

import java.util.List;

public record SubmitAnswersRequest(
	String skillTag,
	String mode,
	List<SubmitAnswerItem> items
) {}