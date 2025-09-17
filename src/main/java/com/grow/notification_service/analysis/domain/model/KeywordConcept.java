package com.grow.notification_service.analysis.domain.model;

import lombok.Getter;

@Getter
public class KeywordConcept {
	private final Long keywordId;
	private final String keywordNormalized; // 정규화된 키워드
	private final String keywordOriginal; // 원본 키워드
	private final String conceptSummary; // 요약 내용

	public KeywordConcept(String keywordNormalized, String keywordOriginal, String conceptSummary) {
		this(null, keywordNormalized, keywordOriginal, conceptSummary);
	}

	public KeywordConcept(Long keywordId, String keywordNormalized, String keywordOriginal, String conceptSummary) {
		this.keywordId = keywordId;
		this.keywordNormalized = keywordNormalized;
		this.keywordOriginal = keywordOriginal;
		this.conceptSummary = conceptSummary;
	}
}