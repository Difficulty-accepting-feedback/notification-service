package com.grow.notification_service.analysis.presentation.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.notification_service.analysis.application.service.AiReviewAnalysisTrigger;
import com.grow.notification_service.analysis.application.service.AiReviewQueryService;
import com.grow.notification_service.analysis.application.service.AiReviewSessionQueryService;
import com.grow.notification_service.analysis.application.service.AnalysisApplicationService;
import com.grow.notification_service.analysis.application.service.QuizGenerationApplicationService;
import com.grow.notification_service.analysis.domain.model.Analysis;
import com.grow.notification_service.analysis.presentation.controller.dto.AiReviewSessionDetailResponse;
import com.grow.notification_service.analysis.presentation.controller.dto.AnalysisResponse;
import com.grow.notification_service.analysis.presentation.controller.dto.QuizItemWithSession;
import com.grow.notification_service.analysis.presentation.controller.dto.QuizResponse;
import com.grow.notification_service.analysis.presentation.controller.dto.RoadmapAnalyzeRequest;
import com.grow.notification_service.global.dto.RsData;
import com.grow.notification_service.quiz.application.dto.QuizItem;
import com.grow.notification_service.quiz.domain.model.Quiz;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

	private final AnalysisApplicationService service;
	private final QuizGenerationApplicationService quizService;
	private final ObjectMapper objectMapper;
	private final AiReviewQueryService aiReviewQueryService;
	private final AiReviewAnalysisTrigger aiReviewAnalysisTrigger;
	private final AiReviewSessionQueryService sessionQueryService;

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
		@RequestParam(defaultValue = "AUTO") String level,
		@RequestParam(required = false) String topic
	) {
		List<Quiz> created = quizService.generateQuizzesFromWrong(memberId, categoryId, level, topic);
		return created.stream().map(QuizResponse::from).toList();
	}

	/**
	 * 최근 생성된 AI 복습 퀴즈 조회 + 세션ID 반환 + (응답 후) 비동기 분석 트리거
	 */
	@GetMapping("/latest")
	public ResponseEntity<RsData<QuizItemWithSession>> getLatest(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestParam("categoryId") Long categoryId,
		@RequestParam(value = "size", required = false) Integer size
	) {
		final int want = (size == null || size <= 0) ? 5 : size;

		// 1) 최근 생성된 퀴즈 아이템
		List<QuizItem> items = aiReviewQueryService.getLatestGenerated(memberId, categoryId, want);

		// 2) 오늘 날짜 범위에서 최신 세션 1건
		java.time.LocalDate today = java.time.LocalDate.now();
		List<AiReviewSessionDetailResponse> sessions = sessionQueryService.getDailySessions(memberId, categoryId, today);
		String sessionId = sessions.isEmpty() ? null : sessions.get(0).sessionId();

		QuizItemWithSession body = new QuizItemWithSession(sessionId, items, java.time.LocalDateTime.now());
		ResponseEntity<RsData<QuizItemWithSession>> response =
			ResponseEntity.ok(new RsData<>("200", "AI 복습 퀴즈 조회 성공", body));

		// 3) 비동기 분석 트리거 (세션ID 포함)
		aiReviewAnalysisTrigger.triggerAfterResponse(memberId, categoryId, items, sessionId);

		return response;
	}

	/** 세션 단건 상세: 퀴즈 + 분석(있으면) */
	@GetMapping("/session/{sessionId}")
	public AiReviewSessionDetailResponse getSessionDetail(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@PathVariable String sessionId
	) {
		return sessionQueryService.getSessionDetail(memberId, sessionId);
	}

	/** 기간 세션 리스트: from~to(포함) 사이 생성된 AI퀴즈+분석 묶음들 */
	@GetMapping("/sessions")
	public List<AiReviewSessionDetailResponse> getSessions(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestParam(required = false) Long categoryId,
		@RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam("to")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		return sessionQueryService.getSessions(memberId, categoryId, from, to);
	}

	/** 그룹 선택 후 스킬태그 기반 로드맵 생성 또는 조회 */
	@PostMapping("/roadmap")
	public AnalysisResponse runRoadmap(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestBody @Valid RoadmapAnalyzeRequest req
	) {
		// category, groupId 전달
		Analysis analysis = service.analyze(memberId, req.getCategory(), req.getGroupId());
		return AnalysisResponse.from(analysis, objectMapper);
	}
}