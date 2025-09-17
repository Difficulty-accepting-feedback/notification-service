package com.grow.notification_service.analysis.infra.persistence.repository;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.grow.notification_service.analysis.domain.model.KeywordConcept;
import com.grow.notification_service.analysis.domain.repository.KeywordConceptRepository;
import com.grow.notification_service.analysis.infra.persistence.entity.KeywordConceptJpaEntity;
import com.grow.notification_service.analysis.infra.persistence.mapper.KeywordConceptMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class KeywordConceptRepositoryImpl implements KeywordConceptRepository {

	private final KeywordConceptJpaRepository jpa;
	private final KeywordConceptMapper mapper;

	/**
	 * 키워드 정규화 값으로 키워드 개념을 조회합니다.(단건)
	 * @param key 키워드 정규화 값
	 * @return 키워드 개념 (없을 경우 빈 Optional)
	 */
	@Override
	public Optional<KeywordConcept> findByKeywordNormalized(String key) {
		return jpa.findByKeywordNormalized(key).map(mapper::toDomain);
	}

	/**
	 * 키워드 정규화 값 집합으로 키워드 개념들을 조회합니다.
	 * @param keys 키워드 정규화 값 집합
	 * @return 키워드 정규화 값을 키로, 키워드 개념을 값으로 하는 맵
	 */
	@Override
	public Map<String, KeywordConcept> findByKeywordNormalizedIn(Set<String> keys) {
		return jpa.findByKeywordNormalizedIn(new ArrayList<>(keys)).stream()
			.map(mapper::toDomain)
			.collect(Collectors.toMap(KeywordConcept::getKeywordNormalized, x -> x));
	}

	/**
	 * 키워드 개념을 삽입하거나 업데이트합니다.
	 * @param concept 삽입하거나 업데이트할 키워드 개념
	 * @return 삽입되거나 업데이트된 키워드 개념
	 */
	@Override
	public KeywordConcept upsert(KeywordConcept concept) {
		return jpa.findByKeywordNormalized(concept.getKeywordNormalized())
			.map(existing -> {
				KeywordConceptJpaEntity updated = KeywordConceptJpaEntity.builder()
					.keywordId(existing.getKeywordId())
					.keywordNormalized(existing.getKeywordNormalized())
					.keywordOriginal(concept.getKeywordOriginal())
					.conceptSummary(concept.getConceptSummary())
					.version(existing.getVersion())
					.build();

				KeywordConceptJpaEntity saved = jpa.save(updated);      // ← merge + @Version 체크
				return mapper.toDomain(saved);
			})
			.orElseGet(() -> {
				// 없는 경우 INSERT 동시 INSERT 경합 대비해 UNIQUE 충돌 시 재조회로 복구
				try {
					KeywordConceptJpaEntity saved = jpa.save(mapper.toEntity(concept));
					return mapper.toDomain(saved);
				} catch (org.springframework.dao.DataIntegrityViolationException e) {
					return jpa.findByKeywordNormalized(concept.getKeywordNormalized())
						.map(mapper::toDomain)
						.orElseThrow(() -> e);
				}
			});
	}
}