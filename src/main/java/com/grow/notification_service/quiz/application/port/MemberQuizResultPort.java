package com.grow.notification_service.quiz.application.port;

import java.util.List;

public interface MemberQuizResultPort {
	/**
	 * 정답인 퀴즈 ID 목록 조회
	 * @param memberId 회원 ID
	 * @return 정답인 퀴즈 ID 목록
	 */
	List<Long> findCorrectQuizIds(Long memberId);

	/**
	 * 특정 카테고리에서 맞춘/틀린 퀴즈 ID 목록 조회
	 * @param memberId 회원 ID
	 * @param categoryId 카테고리 ID
	 * @param correct 맞춘 여부 (true: 맞춘 퀴즈, false: 틀린 퀴즈, null: 전체)
	 * @return 퀴즈 ID 목록
	 */
	List<Long> findAnsweredQuizIds(Long memberId, Long categoryId, Boolean correct);
}