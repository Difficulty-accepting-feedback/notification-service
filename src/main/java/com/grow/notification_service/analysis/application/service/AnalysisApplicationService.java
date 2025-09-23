package com.grow.notification_service.analysis.application.service;

import java.util.List;

import com.grow.notification_service.analysis.domain.model.Analysis;

public interface AnalysisApplicationService {
	Analysis analyze(Long memberId, String category, Long groupId);
	Analysis analyzeQuiz(Long memberId, Long categoryId);
	void analyzeFromQuizIds(Long memberId, Long categoryId, String sessionId, List<Long> quizIds);
}