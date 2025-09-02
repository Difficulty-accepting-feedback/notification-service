package com.grow.notification_service.qna.presentation.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grow.notification_service.global.dto.RsData;
import com.grow.notification_service.qna.application.service.QnaCommandService;
import com.grow.notification_service.qna.presentation.dto.CreateAnswerRequest;
import com.grow.notification_service.qna.presentation.dto.CreateQuestionRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/qna")
// @Tag(name = "QNA", description = "QNA(질문/답변) API")
@Validated
public class QnaController {

	private final QnaCommandService commandService;

	// @Operation(summary = "질문 작성(루트/추가질문)", description = "parentId가 null이면 루트 질문, 값이 있으면 해당 ANSWER 밑 추가 질문입니다.")
	@PostMapping("/questions")
	public ResponseEntity<RsData<Void>> createQuestion(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@Valid @RequestBody CreateQuestionRequest req
	) {
		Long id = commandService.createQuestion(memberId, req.content(), req.parentId());
		return ResponseEntity
			.created(URI.create("/api/v1/qna/questions/" + id))  // Location만 제공
			.body(new RsData<>("201", "질문 작성 성공", null));
	}

	// @Operation(summary = "답변 작성(관리자만)", description = "parent인 questionId는 반드시 QUESTION이어야 합니다.")
	@PostMapping("/questions/{questionId}/answers")
	public ResponseEntity<RsData<Void>> createAnswer(
		@RequestHeader("X-Authorization-Id") Long memberId,
		@PathVariable Long questionId,
		@Valid @RequestBody CreateAnswerRequest req
	) {
		Long id = commandService.createAnswer(memberId, questionId, req.content());
		return ResponseEntity
			.created(URI.create("/api/v1/qna/questions/" + questionId + "/answers/" + id))
			.body(new RsData<>("201", "답변 작성 성공", null));
	}
}