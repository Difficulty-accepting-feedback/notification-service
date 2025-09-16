package com.grow.notification_service.analysis.domain.repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.grow.notification_service.analysis.domain.model.KeywordConcept;

public interface KeywordConceptRepository {
	Optional<KeywordConcept> findByKeywordNormalized(String key);
	Map<String, KeywordConcept> findByKeywordNormalizedIn(Set<String> keys);
	KeywordConcept upsert(KeywordConcept concept);
}