package com.grow.notification_service.analysis.application.service.impl;

import java.util.List;
import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.grow.notification_service.analysis.application.event.AiReviewViewedEvent;
import com.grow.notification_service.analysis.application.service.AiReviewAnalysisTrigger;
import com.grow.notification_service.quiz.application.dto.QuizItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiReviewAnalysisTriggerImpl implements AiReviewAnalysisTrigger {

	private final ApplicationEventPublisher publisher;

	/**
	 * AI 리뷰 분석 트리거 - 퀴즈 응답 후
	 * @param memberId 멤버 ID
	 * @param categoryId 카테고리 ID
	 * @param items 퀴즈 응답 아이템 리스트
	 */
	@Override
	public void triggerAfterResponse(Long memberId, Long categoryId, List<QuizItem> items, String sessionId) {
		// 빈 입력이면 발행 안 함
		if (items == null || items.isEmpty()) return;

		List<Long> quizIds = items.stream()
			.map(QuizItem::quizId)
			.filter(Objects::nonNull)
			.toList();
		if (quizIds.isEmpty()) return;

		// 비동기 리스너가 처리
		publisher.publishEvent(new AiReviewViewedEvent(memberId, categoryId, sessionId, quizIds));
	}
}