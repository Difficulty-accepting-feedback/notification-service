package com.grow.notification_service.analysis.domain.model.eums;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;

@Getter
public class AiReviewSession {
	private final String sessionId;
	private final Long memberId;
	private final Long categoryId;
	private final List<Long> quizIds;
	private final LocalDateTime createdAt;
	private final Long analysisId;

	public AiReviewSession(String sessionId, Long memberId, Long categoryId,
		List<Long> quizIds, LocalDateTime createdAt, Long analysisId) {
		this.sessionId = sessionId;
		this.memberId = memberId;
		this.categoryId = categoryId;
		this.quizIds = quizIds;
		this.createdAt = createdAt;
		this.analysisId = analysisId;
	}

	public AiReviewSession linkAnalysis(Long analysisId) {
		return new AiReviewSession(sessionId, memberId, categoryId, quizIds, createdAt, analysisId);
	}
}