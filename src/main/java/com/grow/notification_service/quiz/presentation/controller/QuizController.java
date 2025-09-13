package com.grow.notification_service.quiz.presentation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.grow.notification_service.global.dto.RsData;
import com.grow.notification_service.quiz.application.dto.QuizItem;
import com.grow.notification_service.quiz.application.service.QuizApplicationService;
import com.grow.notification_service.quiz.presentation.dto.SubmitAnswersRequest;
import com.grow.notification_service.quiz.presentation.dto.SubmitAnswersResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notification/quizzes")
@RequiredArgsConstructor
public class QuizController {

	private final QuizApplicationService appService;

	/**
	 * 난이도별 5문제 출제 (정답 문제 제외)
	 * - mode: EASY | NORMAL | HARD | RANDOM
	 * - skillTag: study_service의 SkillTag.name()
	 */
	@GetMapping
	public RsData<List<QuizItem>> pickByMode(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestParam String skillTag,
		@RequestParam String mode
	) {
		List<QuizItem> data = appService.pickQuizByMode(memberId, skillTag, mode);
		return new RsData<>(
			"200",
			"퀴즈 5문제를 성공적으로 조회했습니다. (mode=" + mode.toUpperCase() + ")",
			data
		);
	}

	@GetMapping("/review")
	public RsData<List<QuizItem>> pickReview(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestParam String skillTag,
		@RequestParam String mode,
		@RequestParam(required = false) Integer total,
		@RequestParam(required = false) Double wrongRatio
	) {
		List<QuizItem> data = appService.pickReviewByHistory(memberId, skillTag, mode, total, wrongRatio);

		return new RsData<>("200", "혼합 출제(틀린 문제 우선) 조회 완료", data);
	}

	/**
	 * 퀴즈 정답 제출
	 * @param memberId - X-Authorization-Id
	 * @param request - SubmitAnswersRequest
	 * @return RsData<SubmitAnswersResponse>
	 */
	@PostMapping("/answers")
	public RsData<SubmitAnswersResponse> submitAnswers(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@RequestBody SubmitAnswersRequest request
	) {
		SubmitAnswersResponse resp = appService.submitAnswers(memberId, request);
		return new RsData<>("200", "정답 제출이 완료되었습니다.", resp);
	}
}