package com.grow.notification_service.analysis.application.service;

import java.util.List;

import com.grow.notification_service.quiz.domain.model.Quiz;

public interface QuizGenerationApplicationService {
	List<Quiz> generateAndSave(Long memberId, Long categoryId, String levelParam, String topic);
	List<Quiz> generateQuizzesFromWrong(Long memberId, Long categoryId, String levelParam, String topic);
}