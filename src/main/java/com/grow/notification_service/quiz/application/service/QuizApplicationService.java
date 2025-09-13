package com.grow.notification_service.quiz.application.service;

import java.util.List;

import com.grow.notification_service.quiz.application.dto.QuizItem;
import com.grow.notification_service.quiz.presentation.dto.SubmitAnswersRequest;
import com.grow.notification_service.quiz.presentation.dto.SubmitAnswersResponse;

public interface QuizApplicationService {
	List<QuizItem> pickQuizByMode(Long memberId, String skillTagCode, String mode);

	SubmitAnswersResponse submitAnswers(Long memberId, SubmitAnswersRequest req);

	List<QuizItem> pickReviewByHistory(
		Long memberId, String skillTagCode, String mode,
		Integer totalOpt, Double wrongRatioOpt
	);
}