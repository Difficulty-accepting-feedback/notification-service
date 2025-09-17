package com.grow.notification_service.analysis.infra.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.grow.notification_service.analysis.infra.persistence.entity.KeywordConceptJpaEntity;

public interface KeywordConceptJpaRepository extends JpaRepository<KeywordConceptJpaEntity, Long> {
	Optional<KeywordConceptJpaEntity> findByKeywordNormalized(String keywordNormalized);
	List<KeywordConceptJpaEntity> findByKeywordNormalizedIn(List<String> keys);
}