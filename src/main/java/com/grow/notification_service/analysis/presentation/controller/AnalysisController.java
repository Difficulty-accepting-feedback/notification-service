package com.grow.notification_service.analysis.presentation.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.notification_service.analysis.application.service.AnalysisApplicationService;
import com.grow.notification_service.analysis.domain.model.Analysis;
import com.grow.notification_service.analysis.presentation.controller.dto.AnalysisResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

	private final AnalysisApplicationService service;

	/**
	 * 자바 프로그래밍 학습 로드맵 분석 요청 (테스트용입니다)
	 */
	@PostMapping
	public AnalysisResponse analyze(
		@RequestParam Long memberId,
		@RequestParam Long categoryId
	) {
		Analysis analysis = service.analyze(memberId, categoryId);
		return AnalysisResponse.from(analysis, new ObjectMapper());
	}
}