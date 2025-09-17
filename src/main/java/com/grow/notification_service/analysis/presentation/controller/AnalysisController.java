package com.grow.notification_service.analysis.presentation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.notification_service.analysis.application.service.AnalysisApplicationService;
import com.grow.notification_service.analysis.application.service.QuizGenerationApplicationService;
import com.grow.notification_service.analysis.domain.model.Analysis;
import com.grow.notification_service.analysis.presentation.controller.dto.AnalysisResponse;
import com.grow.notification_service.analysis.presentation.controller.dto.QuizResponse;
import com.grow.notification_service.quiz.domain.model.Quiz;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

	private final AnalysisApplicationService service;
	private final QuizGenerationApplicationService quizService;
	private final ObjectMapper objectMapper;

	/**
	 * 자바 프로그래밍 학습 로드맵 분석 요청 (테스트용입니다)
	 */
	@PostMapping
	public AnalysisResponse analyze(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestParam Long categoryId
	) {
		Analysis analysis = service.analyze(memberId, categoryId);
		return AnalysisResponse.from(analysis, new ObjectMapper());
	}

	/**
	 * 틀린 퀴즈 기반 분석 요청
	 */
	@PostMapping("/quiz")
	public AnalysisResponse analyzeQuiz(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestParam(required = false) Long categoryId
	) {
		Analysis analysis = service.analyzeQuiz(memberId, categoryId);
		return AnalysisResponse.from(analysis, objectMapper);
	}

	/**
	 * 카테고리, 난이도 별 5문제 생성
	 */
	@PostMapping("/generate")
	public List<Quiz> generate(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestParam Long categoryId,
		@RequestParam(defaultValue = "RANDOM") String level,
		@RequestParam(required = false) String topic
	) {
		return quizService.generateAndSave(memberId, categoryId, level, topic);
	}

	/**
	 * 틀린 문제 기반 퀴즈 생성(5문항) + 저장 후 반환
	 * level: EASY | NORMAL | HARD | RANDOM (기본 RANDOM)
	 * topic: 선택
	 */
	@PostMapping("/generate-from-wrong")
	public List<QuizResponse> generateFromWrong(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestParam Long categoryId,
		@RequestParam(defaultValue = "RANDOM") String level,
		@RequestParam(required = false) String topic
	) {
		List<Quiz> created = quizService.generateQuizzesFromWrong(memberId, categoryId, level, topic);
		return created.stream().map(QuizResponse::from).toList();
	}
}