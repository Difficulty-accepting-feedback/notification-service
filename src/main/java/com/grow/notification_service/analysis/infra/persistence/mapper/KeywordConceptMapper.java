package com.grow.notification_service.analysis.infra.persistence.mapper;

import org.springframework.stereotype.Component;

import com.grow.notification_service.analysis.domain.model.KeywordConcept;
import com.grow.notification_service.analysis.infra.persistence.entity.KeywordConceptJpaEntity;

@Component
public class KeywordConceptMapper {

	public KeywordConcept toDomain(KeywordConceptJpaEntity e) {
		return new KeywordConcept(
			e.getKeywordId(),
			e.getKeywordNormalized(),
			e.getKeywordOriginal(),
			e.getConceptSummary()
		);
	}

	public KeywordConceptJpaEntity toEntity(KeywordConcept d) {
		if (d == null) return null;
		return KeywordConceptJpaEntity.builder()
			.keywordNormalized(d.getKeywordNormalized())
			.keywordOriginal(d.getKeywordOriginal())
			.conceptSummary(d.getConceptSummary())
			.build();
	}
}