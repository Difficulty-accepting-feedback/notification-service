package com.grow.notification_service.analysis.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.grow.notification_service.analysis.domain.model.eums.AiReviewSession;

public interface AiReviewSessionRepository {
	AiReviewSession save(AiReviewSession s);
	Optional<AiReviewSession> findById(String sessionId);
	List<AiReviewSession> findByMemberAndRange(Long memberId, Long categoryId,
		LocalDateTime from, LocalDateTime to);
	void linkAnalysis(String sessionId, Long analysisId);
}