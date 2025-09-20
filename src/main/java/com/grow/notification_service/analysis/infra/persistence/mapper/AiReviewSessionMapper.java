package com.grow.notification_service.analysis.infra.persistence.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.notification_service.analysis.domain.model.eums.AiReviewSession;
import com.grow.notification_service.analysis.infra.persistence.entity.AiReviewSessionJpaEntity;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Component
@RequiredArgsConstructor
public class AiReviewSessionMapper {
	private final ObjectMapper om;

	@SneakyThrows
	public AiReviewSession toDomain(AiReviewSessionJpaEntity e){
		List<Long> ids = om.readValue(e.getQuizIdsJson(), new TypeReference<List<Long>>() {});
		return new AiReviewSession(
			e.getSessionId(),
			e.getMemberId(),
			e.getCategoryId(),
			ids,
			e.getCreatedAt(),
			e.getAnalysisId());
	}

	@SneakyThrows
	public AiReviewSessionJpaEntity toEntity(AiReviewSession d){
		return AiReviewSessionJpaEntity.builder()
			.sessionId(d.getSessionId())
			.memberId(d.getMemberId())
			.categoryId(d.getCategoryId())
			.quizIdsJson(om.writeValueAsString(d.getQuizIds()))
			.createdAt(d.getCreatedAt())
			.analysisId(d.getAnalysisId())
			.build();
	}
}