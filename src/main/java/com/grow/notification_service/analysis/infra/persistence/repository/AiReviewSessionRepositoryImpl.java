package com.grow.notification_service.analysis.infra.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.grow.notification_service.analysis.domain.model.eums.AiReviewSession;
import com.grow.notification_service.analysis.domain.repository.AiReviewSessionRepository;
import com.grow.notification_service.analysis.infra.persistence.entity.AiReviewSessionJpaEntity;
import com.grow.notification_service.analysis.infra.persistence.mapper.AiReviewSessionMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AiReviewSessionRepositoryImpl implements AiReviewSessionRepository {

	private final AiReviewSessionJpaRepository jpa;
	private final AiReviewSessionMapper mapper;

	/**
	 * 세션을 저장합니다.
	 * @param s 세션
	 * @return 저장된 세션
	 */
	@Override
	public AiReviewSession save(AiReviewSession s) {
		return mapper.toDomain(jpa.save(mapper.toEntity(s)));
	}

	/**
	 * 세션 ID로 세션을 조회합니다.
	 * @param sessionId 세션 ID
	 * @return 세션 (없을 경우 빈 Optional)
	 */
	@Override
	public Optional<AiReviewSession> findById(String sessionId) {
		return jpa.findById(sessionId).map(mapper::toDomain);
	}

	/**
	 * 멤버 ID와 카테고리 ID(선택 사항), 그리고 생성일자 범위로 세션 목록을 조회합니다.
	 * @param memberId 멤버 ID
	 * @param categoryId 카테고리 ID (null일 경우 모든 카테고리)
	 * @param from 생성일자 시작
	 * @param to 생성일자 끝
	 * @return 세션 목록
	 */
	@Override
	public List<AiReviewSession> findByMemberAndRange(Long memberId, Long categoryId,
		LocalDateTime from, LocalDateTime to) {
		List<AiReviewSessionJpaEntity> rows = (categoryId == null)
			? jpa.findByMemberIdAndCreatedAtBetweenOrderByCreatedAtDesc(memberId, from, to)
			: jpa.findByMemberIdAndCategoryIdAndCreatedAtBetweenOrderByCreatedAtDesc(memberId, categoryId, from, to);
		return rows.stream().map(mapper::toDomain).toList();
	}

	/**
	 * 세션에 분석 ID를 연결합니다.
	 * @param sessionId 세션 ID
	 * @param analysisId 분석 ID
	 */
	@Override
	public void linkAnalysis(String sessionId, Long analysisId) {
		jpa.findById(sessionId).ifPresent(e -> {
			AiReviewSessionJpaEntity updated = AiReviewSessionJpaEntity.builder()
				.sessionId(e.getSessionId())
				.memberId(e.getMemberId())
				.categoryId(e.getCategoryId())
				.quizIdsJson(e.getQuizIdsJson())
				.createdAt(e.getCreatedAt())
				.analysisId(analysisId)
				.build();
			jpa.save(updated);
		});
	}
}