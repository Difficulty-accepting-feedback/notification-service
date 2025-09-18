package com.grow.notification_service.quiz.application.port;

public interface SubscriptionPort {
	/** 현재 시점에 AI 복습 퀴즈 생성 가능(유효 구독) 여부 */
	boolean canGenerateAiReview(Long memberId);
}