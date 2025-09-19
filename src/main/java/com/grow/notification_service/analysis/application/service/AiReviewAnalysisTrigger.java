package com.grow.notification_service.analysis.application.service;

import java.util.List;

import com.grow.notification_service.quiz.application.dto.QuizItem;

public interface AiReviewAnalysisTrigger {
	void triggerAfterResponse(Long memberId, Long categoryId, List<QuizItem> items);
}