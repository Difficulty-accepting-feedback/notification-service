package com.grow.notification_service.analysis.application.service;

import java.util.List;

import com.grow.notification_service.quiz.application.dto.QuizItem;

public interface AiReviewQueryService {
	List<QuizItem> getLatestGenerated(Long memberId, Long categoryId, Integer sizeOpt);
}