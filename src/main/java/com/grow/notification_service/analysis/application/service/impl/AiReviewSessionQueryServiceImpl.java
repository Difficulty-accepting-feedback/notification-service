package com.grow.notification_service.analysis.application.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.notification_service.analysis.application.service.AiReviewSessionQueryService;
import com.grow.notification_service.analysis.domain.model.Analysis;
import com.grow.notification_service.analysis.domain.model.eums.AiReviewSession;
import com.grow.notification_service.analysis.domain.repository.AiReviewSessionRepository;
import com.grow.notification_service.analysis.infra.persistence.repository.AnalysisJpaRepository;
import com.grow.notification_service.analysis.presentation.controller.dto.AiReviewSessionDetailResponse;
import com.grow.notification_service.global.exception.AnalysisException;
import com.grow.notification_service.global.exception.ErrorCode;
import com.grow.notification_service.quiz.domain.model.Quiz;
import com.grow.notification_service.quiz.domain.repository.QuizRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiReviewSessionQueryServiceImpl implements AiReviewSessionQueryService {

	private final AiReviewSessionRepository sessionRepo;
	private final QuizRepository quizRepository;
	private final AnalysisJpaRepository analysisJpaRepository; // 단순 조회용
	private final ObjectMapper mapper;

	/**
	 * 세션 상세 조회
	 * @param memberId 멤버 ID
	 * @param sessionId 세션 ID
	 * @return 세션 상세
	 */
	@Override
	@Transactional(readOnly = true)
	public AiReviewSessionDetailResponse getSessionDetail(Long memberId, String sessionId) {
		try {
			AiReviewSession s = sessionRepo.findById(sessionId)
				.orElseThrow(() -> new AnalysisException(ErrorCode.ANALYSIS_SESSION_NOT_FOUND));

			// 본인 소유 세션인지 확인
			if (!Objects.equals(s.getMemberId(), memberId)) {
				throw new AnalysisException(ErrorCode.ANALYSIS_FORBIDDEN);
			}

			// 퀴즈 일괄 로드
			Map<Long, Quiz> byId = quizRepository.findByIds(s.getQuizIds()).stream()
				.collect(Collectors.toMap(Quiz::getQuizId, q -> q));

			// 퀴즈 뷰 생성
			List<AiReviewSessionDetailResponse.SessionQuizView> items = s.getQuizIds().stream()
				.map(byId::get)
				.filter(Objects::nonNull)
				.map(AiReviewSessionDetailResponse.SessionQuizView::from) // 정답/해설 포함 뷰
				.toList();

			// 분석 정보
			Analysis analysis = (s.getAnalysisId() == null) ? null :
				analysisJpaRepository.findById(s.getAnalysisId())
					.map(e -> new Analysis(e.getAnalysisId(), e.getMemberId(), e.getCategoryId(),
						e.getSessionId(), e.getAnalysisResult()))
					.orElse(null);

			// 뷰 반환
			return new AiReviewSessionDetailResponse(
				s.getSessionId(), s.getMemberId(), s.getCategoryId(), s.getCreatedAt(),
				items,
				AiReviewSessionDetailResponse.toAnalysisResponseOrNull(analysis, mapper)
			);
		} catch (AnalysisException ae) {
			throw ae;
		} catch (Exception e) {
			throw new AnalysisException(ErrorCode.ANALYSIS_SESSION_QUERY_FAILED, e);
		}
	}

	/**
	 * 날짜 범위 내 세션 리스트
	 * @param memberId 멤버 ID
	 * @param categoryId 카테고리 ID
	 * @param from 기간
	 * @param to 기간
	 * @return 세션 리스트
	 */
	@Override
	@Transactional(readOnly = true)
	public List<AiReviewSessionDetailResponse> getSessions(Long memberId, Long categoryId, LocalDate from, LocalDate to) {
		try {
			if (from == null || to == null || from.isAfter(to)) {
				throw new AnalysisException(ErrorCode.ANALYSIS_INVALID_DATE_RANGE);
			}

			// 날짜 범위의 시작/끝 시각 계산
			LocalDateTime fromTs = from.atStartOfDay();
			LocalDateTime toTs   = to.plusDays(1).atStartOfDay();

			List<AiReviewSession> sessions = sessionRepo.findByMemberAndRange(memberId, categoryId, fromTs, toTs);

			// 퀴즈/분석 일괄 로드
			List<Long> allQuizIds = sessions.stream()
				.flatMap(s -> s.getQuizIds().stream())
				.toList();

			Map<Long, Quiz> quizMap = quizRepository.findByIds(allQuizIds).stream()
				.collect(Collectors.toMap(Quiz::getQuizId, q -> q));

			Map<Long, Analysis> analysisMap = analysisJpaRepository.findAllById(
				sessions.stream()
					.map(AiReviewSession::getAnalysisId)
					.filter(Objects::nonNull)
					.toList()
			).stream().collect(Collectors.toMap(
				e -> e.getAnalysisId(),
				e -> new Analysis(e.getAnalysisId(), e.getMemberId(), e.getCategoryId(), e.getSessionId(), e.getAnalysisResult())
			));

			// 세션별 뷰 생성
			return sessions.stream().map(s -> {
				List<AiReviewSessionDetailResponse.SessionQuizView> items = s.getQuizIds().stream()
					.map(quizMap::get)
					.filter(Objects::nonNull)
					.map(AiReviewSessionDetailResponse.SessionQuizView::from) // 정답/해설 포함 뷰
					.toList();

				// 분석 정보
				Analysis analysis = (s.getAnalysisId() == null) ? null : analysisMap.get(s.getAnalysisId());

				// 뷰 반환
				return new AiReviewSessionDetailResponse(
					s.getSessionId(), s.getMemberId(), s.getCategoryId(), s.getCreatedAt(),
					items,
					AiReviewSessionDetailResponse.toAnalysisResponseOrNull(analysis, mapper)
				);
			}).toList();
		} catch (AnalysisException ae) {
			throw ae;
		} catch (Exception e) {
			throw new AnalysisException(ErrorCode.ANALYSIS_SESSION_QUERY_FAILED, e);
		}
	}

	/**
	 * 오늘 날짜 세션 리스트
	 * @param memberId 멤버 ID
	 * @param categoryId 카테고리 ID
	 * @param date 조회 날짜
	 * @return 세션 리스트
	 */
	@Override
	@Transactional(readOnly = true)
	public List<AiReviewSessionDetailResponse> getDailySessions(Long memberId, Long categoryId, LocalDate date) {
		return getSessions(memberId, categoryId, date, date);
	}
}