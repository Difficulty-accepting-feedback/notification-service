package com.grow.notification_service.analysis.application.service;

import java.time.LocalDate;
import java.util.List;

import com.grow.notification_service.analysis.presentation.controller.dto.AiReviewSessionDetailResponse;

public interface AiReviewSessionQueryService {
	AiReviewSessionDetailResponse getSessionDetail(Long memberId, String sessionId);
	List<AiReviewSessionDetailResponse> getSessions(Long memberId, Long categoryId, LocalDate from, LocalDate to);
	List<AiReviewSessionDetailResponse> getDailySessions(Long memberId, Long categoryId, LocalDate date);
}