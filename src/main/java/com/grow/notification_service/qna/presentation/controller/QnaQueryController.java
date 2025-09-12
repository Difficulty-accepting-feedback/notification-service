package com.grow.notification_service.qna.presentation.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.grow.notification_service.global.dto.RsData;
import com.grow.notification_service.qna.application.dto.QnaPageResponse;
import com.grow.notification_service.qna.application.dto.QnaThreadResponse;
import com.grow.notification_service.qna.application.service.QnaQueryService;
import com.grow.notification_service.qna.domain.model.QnaPost;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notification/qna")
@Tag(name = "QNA 목록 조회", description = "QNA 루트 질문 목록 & 전체 스레드 조회 API (관리자/개인)")
@Validated
public class QnaQueryController {

	private final QnaQueryService queryService;

	// 관리자용
	@Operation(summary = "루트 질문 목록(관리자)", description = "QUESTION & parentId=null 만 최신순 페이징")
	@GetMapping("/questions")
	public ResponseEntity<RsData<QnaPageResponse>> getRootQuestionsAsAdmin(
		@RequestHeader("X-Authorization-Id") Long viewerId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		Page<QnaPost> p = queryService.getRootQuestionsAsAdmin(PageRequest.of(page, size), viewerId);
		return ResponseEntity.ok(new RsData<>("200", "루트 질문 목록 조회 성공", QnaPageResponse.from(p)));
	}

	@Operation(summary = "질의응답 스레드 전체 조회(관리자)", description = "루트 QUESTION 기준으로 모든 자식(Q/A) 트리 반환")
	@GetMapping("/questions/{questionId}/thread")
	public ResponseEntity<RsData<QnaThreadResponse>> getThreadAsAdmin(
		@RequestHeader("X-Authorization-Id") Long viewerId,
		@PathVariable Long questionId
	) {
		QnaThreadResponse body = queryService.getThreadAsAdmin(questionId, viewerId);
		return ResponseEntity.ok(new RsData<>("200", "스레드 조회 성공", body));
	}

	// 개인용

	@Operation(summary = "내 루트 질문 목록", description = "헤더 memberId 소유의 QUESTION & parentId=null 최신순 페이징")
	@GetMapping("/me/questions")
	public ResponseEntity<RsData<QnaPageResponse>> getMyRootQuestions(
		@RequestHeader("X-Authorization-Id") Long viewerId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		Page<QnaPost> p = queryService.getMyRootQuestions(PageRequest.of(page, size), viewerId);
		return ResponseEntity.ok(new RsData<>("200", "내 루트 질문 목록 조회 성공", QnaPageResponse.from(p)));
	}

	@Operation(summary = "내 질의응답 스레드 전체 조회", description = "본인 소유 루트 QUESTION에 한함")
	@GetMapping("/me/questions/{questionId}/thread")
	public ResponseEntity<RsData<QnaThreadResponse>> getMyThread(
		@RequestHeader("X-Authorization-Id") Long viewerId,
		@PathVariable Long questionId
	) {
		QnaThreadResponse body = queryService.getMyThread(questionId, viewerId);
		return ResponseEntity.ok(new RsData<>("200", "내 스레드 조회 성공", body));
	}
}