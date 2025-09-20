package com.grow.notification_service.analysis.presentation.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.notification_service.analysis.domain.model.Analysis;
import com.grow.notification_service.global.exception.AnalysisException;
import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.quiz.domain.model.Quiz;

/**
 * AI 리뷰 세션 상세 응답
 * @param sessionId 세션 ID
 * @param memberId 멤버 ID
 * @param categoryId 카테고리 ID
 * @param createdAt 생성일
 * @param quizzes 퀴즈 목록
 * @param analysis 분석 결과
 */
public record AiReviewSessionDetailResponse(
	String sessionId,
	Long memberId,
	Long categoryId,
	LocalDateTime createdAt,
	List<AiReviewSessionDetailResponse.SessionQuizView> quizzes,
	AnalysisResponse analysis
) {
	/**
	 * Analysis -> AnalysisResponse 변환 (analysis가 null일 경우 null 반환)
	 * @param analysis null 가능
	 * @param mapper ObjectMapper
	 * @return AnalysisResponse or null
	 */
	public static AnalysisResponse toAnalysisResponseOrNull(Analysis analysis, ObjectMapper mapper) {
		if (analysis == null) return null;
		try {
			JsonNode parsed = mapper.readTree(analysis.getAnalysisResult());
			return new AnalysisResponse(
				analysis.getAnalysisId(),
				analysis.getMemberId(),
				analysis.getCategoryId(),
				parsed
			);
		} catch (Exception e) {
			throw new AnalysisException(ErrorCode.ANALYSIS_INPUT_SERIALIZE_FAILED, e);
		}
	}

	/**
	 * Quiz -> QuizResponse 변환
	 * @param q 퀴즈 도메인
	 * @return 퀴즈 응답 DTO
	 */
	public static QuizResponse toQuizResponse(Quiz q) {
		return new QuizResponse(q.getQuizId(), q.getQuestion(), q.getChoices(),
			q.getAnswer(), q.getExplain(), q.getLevel().name(), q.getCategoryId());
	}

	/**
	 * 세션 상세 전용 퀴즈 뷰
	 * - 세션 상세(/api/analysis/session/{sessionId})에서만 해설(explain)을 노출
	 * - 다른 API의 퀴즈 응답은 기존 DTO(해설 미포함)를 유지
	 */
	public static record SessionQuizView(
		Long quizId,
		String question,
		List<String> choices,
		String level,
		Long categoryId,
		String answer,
		String explain
	) {
		public static SessionQuizView from(Quiz q) {
			return new SessionQuizView(
				q.getQuizId(),
				q.getQuestion(),
				q.getChoices(),
				q.getLevel().name(),
				q.getCategoryId(),
				q.getAnswer(),
				q.getExplain()
			);
		}
	}
}