package com.grow.notification_service.quiz.application;

import java.util.List;

public interface MemberQuizResultPort {
	/**
	 * 정답인 퀴즈 ID 목록 조회
	 * @param memberId 회원 ID
	 * @return 정답인 퀴즈 ID 목록
	 */
	List<Long> findCorrectQuizIds(Long memberId);
}