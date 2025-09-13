package com.grow.notification_service.quiz.domain.service;

import java.util.List;

import com.grow.notification_service.quiz.domain.model.Quiz;

public class QuizReviewService {

	public static final int DEFAULT_TOTAL = 5;
	public static final double DEFAULT_WRONG_RATIO = 0.6;
	public static final int MIN_HISTORY = 5;

	public record Need(int wrong, int correct) {}

	/**
	 * 최소 풀이 이력 충족 여부 확인
	 */
	public boolean hasEnoughHistory(List<Quiz> history) {
		return history != null && history.size() >= MIN_HISTORY;
	}

	/**
	 * 필요한 정/오답 개수 계산
	 */
	public Need computeNeeds(int totalOpt, Double wrongRatioOpt) {
		int total = (totalOpt <= 0) ? DEFAULT_TOTAL : totalOpt;
		double wrongRatio = (wrongRatioOpt == null || wrongRatioOpt <= 0.0 || wrongRatioOpt >= 1.0)
			? DEFAULT_WRONG_RATIO : wrongRatioOpt;

		int wrongNeed = (int) Math.ceil(total * wrongRatio);
		if (wrongNeed > total) wrongNeed = total;
		int correctNeed = total - wrongNeed;

		return new Need(wrongNeed, correctNeed);
	}
}