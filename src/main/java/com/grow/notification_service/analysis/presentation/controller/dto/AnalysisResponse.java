package com.grow.notification_service.analysis.presentation.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.notification_service.analysis.domain.model.Analysis;

import lombok.Getter;

public record AnalysisResponse(
	Long analysisId,
	Long memberId,
	Long categoryId,
	JsonNode analysisResult
) {
	public static AnalysisResponse from(Analysis analysis, ObjectMapper mapper) {
		try {
			JsonNode parsed = mapper.readTree(analysis.getAnalysisResult());
			return new AnalysisResponse(
				analysis.getAnalysisId(),
				analysis.getMemberId(),
				analysis.getCategoryId(),
				parsed
			);
		} catch (Exception e) {
			throw new RuntimeException("잘못된 JSON 입니다.", e);
		}
	}
}